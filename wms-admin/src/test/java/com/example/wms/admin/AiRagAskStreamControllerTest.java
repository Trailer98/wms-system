package com.example.wms.admin;

import com.example.wms.admin.knowledge.KnowledgeVectorizer;
import com.example.wms.admin.service.KnowledgeService;
import com.example.wms.admin.view.dto.KnowledgeChunkQuery;
import com.example.wms.admin.view.dto.KnowledgeChunkView;
import com.example.wms.admin.view.dto.KnowledgeDocumentSaveRequest;
import com.example.wms.admin.view.dto.KnowledgeSaveResult;
import com.example.wms.admin.view.dto.LoginRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * HTTP-level checks for POST /ai/rag/ask/stream. A fake {@link VectorStore} + {@link ChatModel} are
 * registered only for this test class (nested {@code @TestConfiguration}, auto-imported by Spring
 * Boot) so the real SSE/Flux plumbing can be exercised end to end — references, delta, done, in order —
 * without needing a live pgvector/Ollama instance or a real DeepSeek API key. Spring Boot's own
 * {@code ChatClientAutoConfiguration} wires a {@code ChatClient.Builder} automatically once a
 * {@link ChatModel} bean exists, so {@link com.example.wms.admin.service.AiRagAskService} needs no
 * test-only code path of its own.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.datasource.url=jdbc:mysql://localhost:3306/wms_system_test?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Tokyo&useSSL=false&allowPublicKeyRetrieval=true"
)
class AiRagAskStreamControllerTest {

    @TestConfiguration
    static class StreamFakesConfig {
        @Bean
        FakeVectorStore fakeVectorStore() {
            return new FakeVectorStore();
        }

        @Bean
        ChatModel fakeChatModel() {
            return new FakeChatModel();
        }
    }

    /** Returns whatever {@link #canned} currently holds — set per-test to control hit/no-hit scenarios. */
    static class FakeVectorStore implements VectorStore {
        volatile List<Document> canned = List.of();

        @Override
        public void add(List<Document> documents) {
        }

        @Override
        public void delete(List<String> ids) {
        }

        @Override
        public void delete(Filter.Expression expression) {
        }

        @Override
        public List<Document> similaritySearch(SearchRequest request) {
            return canned;
        }
    }

    /** Emits three canned tokens via stream(), proving the real delta-by-delta SSE path end to end. */
    static class FakeChatModel implements ChatModel {
        @Override
        public ChatResponse call(Prompt prompt) {
            return new ChatResponse(List.of(new Generation(new AssistantMessage("测试完整回答"))));
        }

        @Override
        public Flux<ChatResponse> stream(Prompt prompt) {
            return Flux.just("这是", "测试", "回答")
                    .map(token -> new ChatResponse(List.of(new Generation(new AssistantMessage(token)))));
        }
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private KnowledgeService knowledgeService;
    @Autowired
    private FakeVectorStore fakeVectorStore;

    private String adminToken;

    @BeforeEach
    void setUp() {
        fakeVectorStore.canned = List.of();
        adminToken = login("admin", "admin123");
    }

    @Test
    void streamWithHitsSendsMetaThenReferencesThenDeltaThenDone() throws Exception {
        seedKnowledgeChunkAndConfigureFakeHit(0.9); // well above the 0.65 default threshold

        String body = postStream("为什么出库锁库后库存没有减少？");

        int metaIdx = firstIndexOfEvent(body, "meta");
        int refIdx = firstIndexOfEvent(body, "references");
        int deltaIdx = firstIndexOfEvent(body, "delta");
        int doneIdx = firstIndexOfEvent(body, "done");
        assertTrue(metaIdx >= 0, "meta event missing:\n" + body);
        assertTrue(refIdx > metaIdx, "references must come after meta:\n" + body);
        assertTrue(deltaIdx > refIdx, "delta must come after references:\n" + body);
        assertTrue(doneIdx > deltaIdx, "done must come after delta:\n" + body);
        assertTrue(body.contains("\"answerability\":\"ANSWERABLE\""), "meta must report ANSWERABLE:\n" + body);
        assertTrue(body.contains("\"rawHitCount\":1") && body.contains("\"validHitCount\":1"),
                "meta must report 1 raw hit and 1 valid hit (0.9 >= 0.65):\n" + body);
        assertTrue(body.contains("\"similarityThreshold\":0.65"));
        assertTrue(body.contains("\"answer\":\"这是测试回答\""), "done must carry the fully reassembled answer:\n" + body);
        assertTrue(body.contains("\"model\":\"deepseek-chat\""), "done must echo the configured model:\n" + body);
    }

    @Test
    void streamWithNoHitsSkipsDeltaAndSendsDoneDirectly() throws Exception {
        String body = postStream("完全不相关的问题-" + System.nanoTime());

        int metaIdx = firstIndexOfEvent(body, "meta");
        int refIdx = firstIndexOfEvent(body, "references");
        assertTrue(metaIdx >= 0, "meta event missing:\n" + body);
        assertTrue(refIdx > metaIdx, "references must come after meta:\n" + body);
        assertTrue(body.contains("\"answerability\":\"NOT_FOUND\""), "meta must report NOT_FOUND:\n" + body);
        assertTrue(body.contains("\"rawHitCount\":0") && body.contains("\"validHitCount\":0"));
        assertFalse(body.contains("event:delta") || body.contains("event: delta"),
                "must never call the LLM when there are no hits:\n" + body);
        assertTrue(firstIndexOfEvent(body, "done") >= 0);
        assertTrue(body.contains("当前知识库没有检索到足够相关的规则"));
    }

    @Test
    void streamWithBelowThresholdHitIsTreatedAsNotFoundAndNeverStreamed() throws Exception {
        // A real hit exists (rawHitCount=1) but its score (0.50) is below the 0.65 threshold — this is
        // exactly the reported bug scenario: a low-similarity hit must never become a reference, never
        // reach the prompt, and must never trigger an LLM call, even though the vector search itself
        // succeeded and returned something.
        seedKnowledgeChunkAndConfigureFakeHit(0.50);

        String body = postStream("系统如何生成财务事件并与 ERP 进行同步");

        assertTrue(body.contains("\"answerability\":\"NOT_FOUND\""), "a 0.50 score must not clear the 0.65 threshold:\n" + body);
        assertTrue(body.contains("\"rawHitCount\":1") && body.contains("\"validHitCount\":0"),
                "rawHitCount must reflect the real hit while validHitCount stays 0:\n" + body);
        assertFalse(body.contains("event:delta") || body.contains("event: delta"),
                "a below-threshold hit must never reach the LLM:\n" + body);
        // references event must carry an empty array — the low-score hit must never be surfaced.
        int refIdx = firstIndexOfEvent(body, "references");
        int doneIdx = firstIndexOfEvent(body, "done");
        String referencesBlock = body.substring(refIdx, doneIdx);
        assertTrue(referencesBlock.contains("\"references\":[]"), "the below-threshold hit must not appear as a reference:\n" + referencesBlock);
    }

    @Test
    void streamBlankQuestionReturnsBadRequestNotSse() throws Exception {
        HttpResponse<String> response = rawPost("{\"question\":\"\"}");
        assertEquals(400, response.statusCode());
    }

    @Test
    void nonStreamAskBelowThresholdReturnsNotFoundWithEmptyReferences() throws Exception {
        // Mirrors the reported bug: a real hit is retrieved (rawHitCount=1) but its score (0.53) is
        // below the 0.65 threshold, so it must not become a reference, must not reach the prompt, and
        // must not trigger an LLM call — /ai/rag/ask must behave identically to the stream endpoint.
        seedKnowledgeChunkAndConfigureFakeHit(0.53);

        String responseBody = postAsk("系统如何生成财务事件并与 ERP 进行同步");
        assertTrue(responseBody.contains("\"answerability\":\"NOT_FOUND\""), responseBody);
        assertTrue(responseBody.contains("\"rawHitCount\":1"), responseBody);
        assertTrue(responseBody.contains("\"validHitCount\":0"), responseBody);
        assertTrue(responseBody.contains("\"references\":[]"), "the below-threshold hit must not be surfaced as a reference:\n" + responseBody);
        assertTrue(responseBody.contains("当前知识库没有检索到足够相关的规则"));
    }

    @Test
    void nonStreamAskAboveThresholdReturnsAnswerableWithReferences() throws Exception {
        seedKnowledgeChunkAndConfigureFakeHit(0.9);

        String responseBody = postAsk("为什么出库锁库后库存没有减少？");
        assertTrue(responseBody.contains("\"answerability\":\"ANSWERABLE\""), responseBody);
        assertTrue(responseBody.contains("\"rawHitCount\":1"), responseBody);
        assertTrue(responseBody.contains("\"validHitCount\":1"), responseBody);
        assertFalse(responseBody.contains("\"references\":[]"), "an above-threshold hit must be surfaced as a reference:\n" + responseBody);
    }

    private String postAsk(String question) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url("/ai/rag/ask")))
                .header("Authorization", "Bearer " + adminToken)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(15))
                .POST(HttpRequest.BodyPublishers.ofString("{\"question\":\"" + question + "\",\"topK\":3}"))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        return response.body();
    }

    private void seedKnowledgeChunkAndConfigureFakeHit(double score) {
        String suffix = String.valueOf(System.nanoTime());
        KnowledgeDocumentSaveRequest request = new KnowledgeDocumentSaveRequest(
                "流式测试知识-" + suffix, "STREAM_TEST_" + suffix, "OUTBOUND", "RULE",
                "OUTBOUND_ORDER", "OUTBOUND_LOCK", "outbound_order", "流式测试场景",
                List.of("出库", "锁库"), "v1.0", "MARKDOWN", "NONE", false, "ENABLED",
                "锁库只增加 locked_qty，不扣减 on_hand_qty。");
        KnowledgeSaveResult created = knowledgeService.create(request);
        KnowledgeChunkView chunk = knowledgeService.pageChunks(created.id(), new KnowledgeChunkQuery()).records().get(0);

        fakeVectorStore.canned = List.of(Document.builder()
                .id("fake-vector-row-1")
                .text(chunk.content())
                .metadata(Map.of(
                        KnowledgeVectorizer.META_CHUNK_ID, String.valueOf(chunk.id()),
                        KnowledgeVectorizer.META_DOC_ID, String.valueOf(created.id())))
                .score(score)
                .build());
    }

    /** Finds "event:<name>" or "event: <name>" regardless of the SSE writer's exact spacing. */
    private int firstIndexOfEvent(String body, String name) {
        int noSpace = body.indexOf("event:" + name);
        int withSpace = body.indexOf("event: " + name);
        if (noSpace < 0) return withSpace;
        if (withSpace < 0) return noSpace;
        return Math.min(noSpace, withSpace);
    }

    private String postStream(String question) throws Exception {
        HttpResponse<String> response = rawPost("{\"question\":\"" + question + "\",\"topK\":3}");
        assertEquals(200, response.statusCode());
        return response.body();
    }

    private HttpResponse<String> rawPost(String jsonBody) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url("/ai/rag/ask/stream")))
                .header("Authorization", "Bearer " + adminToken)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(15))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private String login(String username, String password) {
        ResponseEntity<String> response = restTemplate.postForEntity(
                url("/auth/login"), new LoginRequest(username, password), String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        String body = response.getBody();
        int start = body.indexOf("\"token\":\"") + "\"token\":\"".length();
        int end = body.indexOf('"', start);
        return body.substring(start, end);
    }

    private String url(String path) {
        return "http://localhost:" + port + "/api" + path;
    }
}
