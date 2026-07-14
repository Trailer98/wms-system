package com.example.wms.admin.service;

import com.example.wms.admin.config.RagProperties;
import com.example.wms.admin.security.CurrentUser;
import com.example.wms.admin.security.CurrentUserContext;
import com.example.wms.admin.view.dto.AiRagAskRequest;
import com.example.wms.admin.view.dto.AiRagAskResponse;
import com.example.wms.admin.view.dto.AiRagReferenceVO;
import com.example.wms.admin.view.dto.AiRagStreamDeltaEvent;
import com.example.wms.admin.view.dto.AiRagStreamDoneEvent;
import com.example.wms.admin.view.dto.AiRagStreamErrorEvent;
import com.example.wms.admin.view.dto.AiRagStreamMetaEvent;
import com.example.wms.admin.view.dto.AiRagStreamReferencesEvent;
import com.example.wms.admin.view.dto.KnowledgeSearchHit;
import com.example.wms.admin.view.dto.KnowledgeSearchRequest;
import com.example.wms.common.common.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.deepseek.DeepSeekChatOptions;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;

/**
 * P0 RAG question-answering: retrieves knowledge chunks via {@link KnowledgeService#search} (MySQL
 * master data resolved through the existing pgvector-backed retrieval — no separate vector search
 * logic here), filters them by {@code app.ai.rag.similarity-threshold}, then asks DeepSeek Chat to
 * answer using ONLY the surviving ("valid") hits. Static knowledge only: never touches inventory /
 * inbound / outbound / stock-movement / exception / finance services.
 *
 * <p><b>Score semantics.</b> Spring AI's {@code PgVectorStore} (see {@code AiVectorStoreConfig}, which
 * configures {@code PgDistanceType.COSINE_DISTANCE}) runs {@code embedding <=> ?} (pgvector's cosine
 * *distance* operator, aliased {@code AS distance} in its generated SQL) but its own
 * {@code DocumentRowMapper} converts that to a *similarity* score — {@code Document.score = 1.0 -
 * distance} — before this service, {@link KnowledgeVectorizer}, or {@link KnowledgeService} ever see
 * it. So {@link KnowledgeSearchHit#score()} arriving here is already {@code similarityScore} (higher =
 * more relevant); there is no separate raw distance value available at this layer to convert or log.
 *
 * <p>The {@link ChatClient.Builder} bean only exists when the {@code ai} profile's DeepSeek chat model
 * is configured, so it is injected as an optional {@link ObjectProvider} — same pattern as
 * {@code KnowledgeVectorizer}'s optional {@code VectorStore}. Under the {@code test} profile (chat model
 * disabled) this service is still fully constructible and callable; every retrieval failure or missing
 * chat client degrades to a clear message instead of a fake answer or a broken bean graph.
 *
 * <p>{@link RagAudienceMode} (business vs. technical wording) is decided purely from
 * {@link CurrentUserContext} — specifically whether the caller's {@code roleCodes} contains
 * {@code DEVELOPER} (see V9 migration) — never from any client-supplied request field, so a caller
 * can't self-elevate to the technical mode.
 */
@Service
public class AiRagAskService {

    private static final Logger log = LoggerFactory.getLogger(AiRagAskService.class);

    /** Hard safety ceiling on topK; app.ai.rag has no configurable max yet. */
    private static final int MAX_TOP_K = 8;
    private static final int MAX_CHUNK_CONTEXT_CHARS = 1200;
    private static final int MAX_TOTAL_CONTEXT_CHARS = 6000;
    private static final int PREVIEW_CHARS = 160;
    private static final double DEFAULT_TEMPERATURE = 0.2;
    /** Generous ceiling for a full RAG generation over SSE; 0 would mean "never time out". */
    private static final long STREAM_TIMEOUT_MS = 180_000L;

    private static final String DEVELOPER_ROLE_CODE = "DEVELOPER";

    /**
     * Fixed refusal text for {@link Answerability#NOT_FOUND}. Deliberately never phrased as a Prompt
     * instruction to the model — a below-threshold hit never reaches the LLM at all, so there is
     * nothing for a prompt to "hide"; this is the entire answer.
     */
    private static final String REJECTION_ANSWER =
            "当前知识库没有检索到足够相关的规则，无法基于当前系统知识回答。请补充对应知识，或确认该功能是否已经在当前系统中实现。"
                    + "不能根据通用行业经验推断当前系统已支持该功能。";

    private static final String SYSTEM_PROMPT_BUSINESS = """
            你是一个 WMS（仓储管理系统）智能助手。
            你的默认用户是仓库操作员、仓库主管、运营人员等非开发人员。
            回答时优先使用中文业务术语，不要默认暴露数据库字段名、表名、枚举值或接口名。
            请使用"现存库存、锁定库存、冻结库存、可用库存、出库单、锁库、确认发货"等业务语言。
            如果必须解释字段，请用中文名称说明，不要堆砌英文字段。
            不要编造实时库存、订单、流水或异常事件数据。
            如果问题涉及实时库存、订单、出库单、入库单、流水或异常事件，请说明当前 RAG 问答只基于静态知识规则，实时数据需要通过业务查询工具获取。
            """;

    private static final String SYSTEM_PROMPT_TECHNICAL = """
            你是一个 WMS（仓储管理系统）技术助手。
            用户是开发、测试、实施或技术调试人员。
            回答时可以使用系统字段名、枚举值、业务动作和技术术语，例如 on_hand_qty、locked_qty、frozen_qty、available_qty、OUTBOUND_LOCK、OUTBOUND_SHIP。
            但仍需先给结论，再说明原因。
            如果涉及库存变化，应明确说明相关字段如何变化。
            不要编造实时库存、订单、流水或异常事件数据。
            如果问题涉及实时数据，请说明当前 RAG 问答只基于静态知识，实时数据需要通过 Tool Calling 或业务查询接口获取。
            """;

    private final KnowledgeService knowledgeService;
    private final ObjectProvider<ChatClient.Builder> chatClientBuilderProvider;
    private final String chatModel;
    private final RagProperties ragProperties;

    public AiRagAskService(KnowledgeService knowledgeService,
                            ObjectProvider<ChatClient.Builder> chatClientBuilderProvider,
                            @Value("${spring.ai.deepseek.chat.model:deepseek-chat}") String chatModel,
                            RagProperties ragProperties) {
        this.knowledgeService = knowledgeService;
        this.chatClientBuilderProvider = chatClientBuilderProvider;
        this.chatModel = chatModel;
        this.ragProperties = ragProperties;
    }

    public AiRagAskResponse ask(AiRagAskRequest request) {
        if (request == null || !StringUtils.hasText(request.question())) {
            throw new BusinessException("问题 question 不能为空");
        }
        String question = request.question().strip();
        int topK = resolveTopK(request.topK());
        RagAudienceMode mode = resolveAudienceMode();
        double threshold = ragProperties.getSimilarityThreshold();

        Retrieval retrieval = retrieve(question, request.module(), request.operationType(), topK, threshold);

        if (retrieval.validHits().isEmpty()) {
            String answer = retrieval.unavailableReason() != null
                    ? "知识检索当前不可用（" + retrieval.unavailableReason() + "），未基于知识库生成回答。"
                    : REJECTION_ANSWER;
            return new AiRagAskResponse(answer, question, List.of(), topK, 0, null, mode, mode.label(),
                    Answerability.NOT_FOUND, Answerability.NOT_FOUND.label(), threshold,
                    retrieval.rawHits().size(), 0);
        }

        List<KnowledgeSearchHit> validHits = retrieval.validHits();
        List<AiRagReferenceVO> references = validHits.stream().map(this::toReference).toList();
        ContextResult context = buildContext(validHits);

        ChatClient.Builder builder = chatClientBuilderProvider.getIfAvailable();
        if (builder == null) {
            return new AiRagAskResponse(
                    "知识检索已完成，但当前未启用大模型（未激活 ai profile 或未配置 DeepSeek），无法生成回答。",
                    question, references, topK, context.usedCount(), null, mode, mode.label(),
                    Answerability.ANSWERABLE, Answerability.ANSWERABLE.label(), threshold,
                    retrieval.rawHits().size(), validHits.size());
        }

        double temperature = request.temperature() != null ? request.temperature() : DEFAULT_TEMPERATURE;
        String userPrompt = buildUserPrompt(question, context.text(), mode);
        try {
            String answer = builder.build()
                    .prompt()
                    .system(systemPromptFor(mode))
                    .user(userPrompt)
                    .options(DeepSeekChatOptions.builder().temperature(temperature).build())
                    .call()
                    .content();
            return new AiRagAskResponse(answer, question, references, topK, context.usedCount(), chatModel, mode, mode.label(),
                    Answerability.ANSWERABLE, Answerability.ANSWERABLE.label(), threshold,
                    retrieval.rawHits().size(), validHits.size());
        } catch (RuntimeException ex) {
            log.warn("RAG ask: LLM call failed: {}", ex.getMessage());
            return new AiRagAskResponse(
                    "知识检索已完成，但调用大模型生成回答失败：" + rootMessage(ex),
                    question, references, topK, context.usedCount(), chatModel, mode, mode.label(),
                    Answerability.ANSWERABLE, Answerability.ANSWERABLE.label(), threshold,
                    retrieval.rawHits().size(), validHits.size());
        }
    }

    /**
     * Streaming counterpart of {@link #ask}: same retrieval, filtering, and prompt-building, but
     * pushes results over SSE as they become available. Order is: retrieve + filter first, then
     * {@code meta} (carrying the retrieval stats), then {@code references}/{@code delta}/{@code done}.
     *
     * <p>Search and filtering run synchronously on the calling (request) thread before the emitter is
     * returned to Spring MVC — everything sent before that point is buffered by {@link SseEmitter} and
     * flushed once the async response is initialized, so this is safe. The LLM token stream, once
     * started, is driven entirely by {@link reactor.core.publisher.Flux}'s own (reactor-netty)
     * scheduler via {@code subscribe()}, which is non-blocking — no manual threading or {@code @Async}
     * needed, and the app stays plain Spring MVC (no WebFlux migration).
     */
    public SseEmitter askStream(AiRagAskRequest request) {
        if (request == null || !StringUtils.hasText(request.question())) {
            // Same as ask(): let GlobalExceptionHandler turn this into a normal 400 JSON response
            // before any SSE emitter/response is ever created.
            throw new BusinessException("问题 question 不能为空");
        }
        String question = request.question().strip();
        int topK = resolveTopK(request.topK());
        RagAudienceMode mode = resolveAudienceMode();
        double threshold = ragProperties.getSimilarityThreshold();
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MS);

        Retrieval retrieval = retrieve(question, request.module(), request.operationType(), topK, threshold);
        boolean answerable = !retrieval.validHits().isEmpty();
        Answerability answerability = answerable ? Answerability.ANSWERABLE : Answerability.NOT_FOUND;

        sendMeta(emitter, mode, answerability, topK, threshold, retrieval.rawHits().size(), retrieval.validHits().size());

        if (!answerable) {
            sendReferences(emitter, List.of());
            String answer = retrieval.unavailableReason() != null
                    ? "知识检索当前不可用（" + retrieval.unavailableReason() + "），未基于知识库生成回答。"
                    : REJECTION_ANSWER;
            sendDone(emitter, answer, null, 0, mode);
            emitter.complete();
            return emitter;
        }

        List<KnowledgeSearchHit> validHits = retrieval.validHits();
        List<AiRagReferenceVO> references = validHits.stream().map(this::toReference).toList();
        sendReferences(emitter, references);

        ContextResult context = buildContext(validHits);
        ChatClient.Builder builder = chatClientBuilderProvider.getIfAvailable();
        if (builder == null) {
            sendError(emitter, "知识检索已完成，但当前未启用大模型（未激活 ai profile 或未配置 DeepSeek），无法生成回答。");
            emitter.complete();
            return emitter;
        }

        double temperature = request.temperature() != null ? request.temperature() : DEFAULT_TEMPERATURE;
        String userPrompt = buildUserPrompt(question, context.text(), mode);
        StringBuilder fullAnswer = new StringBuilder();

        builder.build()
                .prompt()
                .system(systemPromptFor(mode))
                .user(userPrompt)
                .options(DeepSeekChatOptions.builder().temperature(temperature).build())
                .stream()
                .content()
                .subscribe(
                        token -> {
                            fullAnswer.append(token);
                            sendDelta(emitter, token);
                        },
                        ex -> {
                            log.warn("RAG ask stream: LLM streaming failed: {}", ex.getMessage());
                            sendError(emitter, "知识检索已完成，但调用大模型生成回答失败：" + rootMessage(ex));
                            emitter.complete();
                        },
                        () -> {
                            sendDone(emitter, fullAnswer.toString(), chatModel, context.usedCount(), mode);
                            emitter.complete();
                        });

        return emitter;
    }

    private record Retrieval(List<KnowledgeSearchHit> rawHits, List<KnowledgeSearchHit> validHits, String unavailableReason) {
    }

    /**
     * Retrieves rawHits (already capped at topK by {@link KnowledgeService#search}, unfiltered by
     * similarity), computes validHits (rawHits whose similarityScore >= threshold), and logs a
     * retrieval summary. A search failure (e.g. vector store not configured) degrades to an empty
     * rawHits/validHits with {@code unavailableReason} set, rather than propagating — the caller folds
     * that into the same NOT_FOUND shape.
     */
    private Retrieval retrieve(String question, String module, String operationType, int topK, double threshold) {
        List<KnowledgeSearchHit> rawHits;
        String unavailableReason = null;
        try {
            rawHits = knowledgeService.search(new KnowledgeSearchRequest(question, module, operationType, topK)).records();
        } catch (RuntimeException ex) {
            log.warn("RAG ask: knowledge search unavailable: {}", ex.getMessage());
            rawHits = List.of();
            unavailableReason = rootMessage(ex);
        }

        List<KnowledgeSearchHit> validHits = rawHits.stream()
                .filter(hit -> hit.score() != null && hit.score() >= threshold)
                .toList();

        Answerability answerability = validHits.isEmpty() ? Answerability.NOT_FOUND : Answerability.ANSWERABLE;
        log.info("RAG retrieval summary: question={} topK={} similarityThreshold={} rawHitCount={} validHitCount={} rawScores={} validScores={} answerability={}",
                question, topK, threshold, rawHits.size(), validHits.size(),
                rawHits.stream().map(KnowledgeSearchHit::score).toList(),
                validHits.stream().map(KnowledgeSearchHit::score).toList(),
                answerability);

        return new Retrieval(rawHits, validHits, unavailableReason);
    }

    /**
     * TECHNICAL_USER only for callers whose roles (from the current JWT-backed session, never client
     * input) include DEVELOPER; everyone else — including ADMIN without the DEVELOPER role, and
     * unauthenticated/role-less contexts — gets BUSINESS_USER.
     */
    private RagAudienceMode resolveAudienceMode() {
        CurrentUser currentUser = CurrentUserContext.get();
        if (currentUser == null || currentUser.roleCodes() == null) {
            return RagAudienceMode.BUSINESS_USER;
        }
        return currentUser.roleCodes().contains(DEVELOPER_ROLE_CODE)
                ? RagAudienceMode.TECHNICAL_USER
                : RagAudienceMode.BUSINESS_USER;
    }

    /** Public so it can be asserted on directly in tests without needing a real LLM call. */
    public String systemPromptFor(RagAudienceMode mode) {
        return mode == RagAudienceMode.TECHNICAL_USER ? SYSTEM_PROMPT_TECHNICAL : SYSTEM_PROMPT_BUSINESS;
    }

    private void sendMeta(SseEmitter emitter, RagAudienceMode mode, Answerability answerability, int topK,
                           double threshold, int rawHitCount, int validHitCount) {
        sendEvent(emitter, "meta", new AiRagStreamMetaEvent(mode, mode.label(), chatModel,
                answerability, answerability.label(), topK, threshold, rawHitCount, validHitCount));
    }

    private void sendReferences(SseEmitter emitter, List<AiRagReferenceVO> references) {
        sendEvent(emitter, "references", new AiRagStreamReferencesEvent(references));
    }

    private void sendDelta(SseEmitter emitter, String text) {
        sendEvent(emitter, "delta", new AiRagStreamDeltaEvent(text));
    }

    private void sendDone(SseEmitter emitter, String answer, String model, int usedContextCount, RagAudienceMode mode) {
        sendEvent(emitter, "done", new AiRagStreamDoneEvent(answer, model, usedContextCount, mode, mode.label()));
    }

    private void sendError(SseEmitter emitter, String message) {
        sendEvent(emitter, "error", new AiRagStreamErrorEvent(message));
    }

    private void sendEvent(SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data, MediaType.APPLICATION_JSON));
        } catch (IOException ex) {
            // The client most likely disconnected mid-stream; nothing more can be written.
            log.warn("RAG ask stream: failed to send '{}' event, client likely disconnected: {}", eventName, ex.getMessage());
            emitter.completeWithError(ex);
        }
    }

    /**
     * null/missing, zero, or negative -> app.ai.rag.top-k; above the safety ceiling -> clamp to
     * {@link #MAX_TOP_K}; otherwise the caller's value is used as-is. topK only ever bounds how many
     * *candidates* come back from vector search — the similarity-threshold filter that runs afterward
     * never tops back up to topK with lower-scoring hits.
     */
    private int resolveTopK(Integer requested) {
        if (requested == null || requested <= 0) {
            return ragProperties.getTopK();
        }
        return Math.min(requested, MAX_TOP_K);
    }

    private AiRagReferenceVO toReference(KnowledgeSearchHit hit) {
        return new AiRagReferenceVO(hit.chunkId(), hit.docId(), hit.title(), hit.module(),
                hit.sourceType(), hit.operationType(), hit.score(), preview(hit.content()));
    }

    private String preview(String content) {
        if (content == null) {
            return null;
        }
        String stripped = content.strip();
        return stripped.length() <= PREVIEW_CHARS ? stripped : stripped.substring(0, PREVIEW_CHARS) + "…";
    }

    private record ContextResult(String text, int usedCount) {
    }

    /** Packs valid hits into the prompt context, capped at MAX_TOTAL_CONTEXT_CHARS; always includes at least one. */
    private ContextResult buildContext(List<KnowledgeSearchHit> validHits) {
        StringBuilder sb = new StringBuilder();
        int used = 0;
        for (int i = 0; i < validHits.size(); i++) {
            KnowledgeSearchHit hit = validHits.get(i);
            String block = "[知识" + (i + 1) + "]\n"
                    + "标题：" + orDash(hit.title()) + "\n"
                    + "模块：" + orDash(hit.module()) + "\n"
                    + "操作类型：" + orDash(hit.operationType()) + "\n"
                    + "内容：\n" + truncate(hit.content(), MAX_CHUNK_CONTEXT_CHARS) + "\n\n";
            if (used > 0 && sb.length() + block.length() > MAX_TOTAL_CONTEXT_CHARS) {
                break;
            }
            sb.append(block);
            used++;
        }
        return new ContextResult(sb.toString(), used);
    }

    private String buildUserPrompt(String question, String context, RagAudienceMode mode) {
        String qtyInstruction = mode == RagAudienceMode.TECHNICAL_USER
                ? "3. 如果涉及库存数量变化，请说明 on_hand_qty、locked_qty、frozen_qty、available_qty 的具体变化；\n"
                : "3. 如果涉及库存数量变化，请用中文业务术语说明（如现存库存、锁定库存、冻结库存、可用库存的变化），不要堆砌英文字段名；\n";
        return "用户问题：\n" + question + "\n\n"
                + "知识库上下文：\n" + context
                + "请基于以上知识回答用户问题。\n"
                + "回答要求：\n"
                + "1. 先直接回答结论；\n"
                + "2. 再说明原因；\n"
                + qtyInstruction
                + "4. 不要编造知识库中没有的信息；\n"
                + "5. 不要查询或假设实时库存数据；\n"
                + "6. 最后用\"参考知识：xxx、xxx\"列出引用的知识标题。";
    }

    private String truncate(String content, int max) {
        if (content == null) {
            return "";
        }
        String stripped = content.strip();
        return stripped.length() <= max ? stripped : stripped.substring(0, max) + "…";
    }

    private String orDash(String value) {
        return StringUtils.hasText(value) ? value : "-";
    }

    private String rootMessage(Throwable ex) {
        Throwable cursor = ex;
        while (cursor.getCause() != null && cursor.getCause() != cursor) {
            cursor = cursor.getCause();
        }
        return cursor.getMessage() == null ? cursor.getClass().getSimpleName() : cursor.getMessage();
    }
}
