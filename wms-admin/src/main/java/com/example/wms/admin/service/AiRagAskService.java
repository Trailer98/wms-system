package com.example.wms.admin.service;

import com.example.wms.admin.view.dto.AiRagAskRequest;
import com.example.wms.admin.view.dto.AiRagAskResponse;
import com.example.wms.admin.view.dto.AiRagReferenceVO;
import com.example.wms.admin.view.dto.KnowledgeSearchHit;
import com.example.wms.admin.view.dto.KnowledgeSearchRequest;
import com.example.wms.common.common.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.deepseek.DeepSeekChatOptions;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * P0 RAG question-answering: retrieves knowledge chunks via {@link KnowledgeService#search} (MySQL
 * master data resolved through the existing pgvector-backed retrieval — no separate vector search
 * logic here), then asks DeepSeek Chat to answer using ONLY that retrieved context. Static knowledge
 * only: never touches inventory / inbound / outbound / stock-movement / exception / finance services.
 *
 * <p>The {@link ChatClient.Builder} bean only exists when the {@code ai} profile's DeepSeek chat model
 * is configured, so it is injected as an optional {@link ObjectProvider} — same pattern as
 * {@code KnowledgeVectorizer}'s optional {@code VectorStore}. Under the {@code test} profile (chat model
 * disabled) this service is still fully constructible and callable; every retrieval failure or missing
 * chat client degrades to a clear message instead of a fake answer or a broken bean graph.
 */
@Service
public class AiRagAskService {

    private static final Logger log = LoggerFactory.getLogger(AiRagAskService.class);

    private static final int DEFAULT_TOP_K = 3;
    private static final int MAX_TOP_K = 8;
    private static final int MAX_CHUNK_CONTEXT_CHARS = 1200;
    private static final int MAX_TOTAL_CONTEXT_CHARS = 6000;
    private static final int PREVIEW_CHARS = 160;
    private static final double DEFAULT_TEMPERATURE = 0.2;

    private static final String NO_HIT_ANSWER =
            "未检索到足够相关的 WMS 知识，无法基于知识库回答。请补充对应业务规则或调整问题描述。";

    private static final String SYSTEM_PROMPT = """
            你是一个 WMS（仓储管理系统）智能助手。
            你只能基于提供的知识库上下文回答问题。
            如果上下文不足以回答，请明确说明"知识库中没有足够信息"，不要编造实时库存、订单、流水或系统数据。
            回答时使用简洁中文。
            如果问题涉及实时库存、订单、出库单、入库单、流水、异常事件，请说明当前 RAG 问答只基于静态知识规则，实时数据需要通过业务查询工具获取。
            回答中尽量使用系统字段名，例如 on_hand_qty、locked_qty、frozen_qty、available_qty。
            """;

    private final KnowledgeService knowledgeService;
    private final ObjectProvider<ChatClient.Builder> chatClientBuilderProvider;
    private final String chatModel;

    public AiRagAskService(KnowledgeService knowledgeService,
                            ObjectProvider<ChatClient.Builder> chatClientBuilderProvider,
                            @Value("${spring.ai.deepseek.chat.model:deepseek-chat}") String chatModel) {
        this.knowledgeService = knowledgeService;
        this.chatClientBuilderProvider = chatClientBuilderProvider;
        this.chatModel = chatModel;
    }

    public AiRagAskResponse ask(AiRagAskRequest request) {
        if (request == null || !StringUtils.hasText(request.question())) {
            throw new BusinessException("问题 question 不能为空");
        }
        String question = request.question().strip();
        int topK = resolveTopK(request.topK());

        List<KnowledgeSearchHit> hits;
        try {
            hits = knowledgeService.search(new KnowledgeSearchRequest(question, request.module(), request.operationType(), topK)).records();
        } catch (RuntimeException ex) {
            // Knowledge retrieval itself is unavailable (e.g. vector store not configured) — never
            // fake an answer; fold into the same "no usable knowledge" shape the caller already handles.
            log.warn("RAG ask: knowledge search unavailable: {}", ex.getMessage());
            return noHitResponse(question, topK, "知识检索当前不可用（" + rootMessage(ex) + "），未基于知识库生成回答。");
        }

        if (hits.isEmpty()) {
            return noHitResponse(question, topK, NO_HIT_ANSWER);
        }

        List<AiRagReferenceVO> references = hits.stream().map(this::toReference).toList();
        ContextResult context = buildContext(hits);

        ChatClient.Builder builder = chatClientBuilderProvider.getIfAvailable();
        if (builder == null) {
            return new AiRagAskResponse(
                    "知识检索已完成，但当前未启用大模型（未激活 ai profile 或未配置 DeepSeek），无法生成回答。",
                    question, references, topK, context.usedCount(), null);
        }

        double temperature = request.temperature() != null ? request.temperature() : DEFAULT_TEMPERATURE;
        String userPrompt = buildUserPrompt(question, context.text());
        try {
            String answer = builder.build()
                    .prompt()
                    .system(SYSTEM_PROMPT)
                    .user(userPrompt)
                    .options(DeepSeekChatOptions.builder().temperature(temperature).build())
                    .call()
                    .content();
            return new AiRagAskResponse(answer, question, references, topK, context.usedCount(), chatModel);
        } catch (RuntimeException ex) {
            log.warn("RAG ask: LLM call failed: {}", ex.getMessage());
            return new AiRagAskResponse(
                    "知识检索已完成，但调用大模型生成回答失败：" + rootMessage(ex),
                    question, references, topK, context.usedCount(), chatModel);
        }
    }

    private int resolveTopK(Integer requested) {
        if (requested == null) {
            return DEFAULT_TOP_K;
        }
        return Math.min(Math.max(requested, 1), MAX_TOP_K);
    }

    private AiRagAskResponse noHitResponse(String question, int topK, String answer) {
        return new AiRagAskResponse(answer, question, List.of(), topK, 0, null);
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

    /** Packs retrieved chunks into the prompt context, capped at MAX_TOTAL_CONTEXT_CHARS; always includes at least one. */
    private ContextResult buildContext(List<KnowledgeSearchHit> hits) {
        StringBuilder sb = new StringBuilder();
        int used = 0;
        for (int i = 0; i < hits.size(); i++) {
            KnowledgeSearchHit hit = hits.get(i);
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

    private String buildUserPrompt(String question, String context) {
        return "用户问题：\n" + question + "\n\n"
                + "知识库上下文：\n" + context
                + "请基于以上知识回答用户问题。\n"
                + "回答要求：\n"
                + "1. 先直接回答结论；\n"
                + "2. 再说明原因；\n"
                + "3. 如果涉及库存数量变化，请说明 on_hand_qty、locked_qty、frozen_qty、available_qty 的变化；\n"
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
