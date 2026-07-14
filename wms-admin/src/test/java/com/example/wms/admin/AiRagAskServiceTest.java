package com.example.wms.admin;

import com.example.wms.admin.security.CurrentUserContext;
import com.example.wms.admin.service.AiRagAskService;
import com.example.wms.admin.service.Answerability;
import com.example.wms.admin.service.RagAudienceMode;
import com.example.wms.admin.view.dto.AiRagAskRequest;
import com.example.wms.admin.view.dto.AiRagAskResponse;
import com.example.wms.common.common.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs under the {@code test} profile where the embedding model is disabled (application-test.yml:
 * spring.ai.model.embedding=none), so {@code knowledgeService.search()} always fails with "vector store
 * unavailable" — deterministically exercising AiRagAskService's "no usable knowledge => never call the
 * LLM" degrade path without needing a real DeepSeek API key or a live pgvector/Ollama instance.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:mysql://localhost:3306/wms_system_test?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Tokyo&useSSL=false&allowPublicKeyRetrieval=true")
class AiRagAskServiceTest {

    @Autowired
    private AiRagAskService aiRagAskService;

    @Test
    void blankQuestionThrowsBusinessException() {
        assertThrows(BusinessException.class, () -> aiRagAskService.ask(new AiRagAskRequest("   ", null, null, null, null)));
        assertThrows(BusinessException.class, () -> aiRagAskService.ask(null));
    }

    @Test
    void topKDefaultsToThreeWhenNotSpecified() {
        AiRagAskResponse response = aiRagAskService.ask(new AiRagAskRequest("为什么出库锁库后库存没有减少？", null, null, null, null));
        assertEquals(3, response.topK());
    }

    @Test
    void topKAboveEightIsClampedToEight() {
        AiRagAskResponse response = aiRagAskService.ask(new AiRagAskRequest("测试问题", null, null, 50, null));
        assertEquals(8, response.topK());
    }

    @Test
    void topKZeroOrNegativeFallsBackToConfiguredDefault() {
        // Per spec: 0/negative topK is treated the same as "not supplied" — it falls back to
        // app.ai.rag.top-k (3 here), it is NOT clamped up to 1.
        assertEquals(3, aiRagAskService.ask(new AiRagAskRequest("测试问题", null, null, 0, null)).topK());
        assertEquals(3, aiRagAskService.ask(new AiRagAskRequest("测试问题", null, null, -1, null)).topK());
    }

    @Test
    void unavailableKnowledgeRetrievalNeverCallsLlmAndReturnsEmptyReferences() {
        AiRagAskResponse response = aiRagAskService.ask(
                new AiRagAskRequest("为什么出库锁库后库存没有减少？", "OUTBOUND", "OUTBOUND_LOCK", 3, null));

        assertNotNull(response.answer());
        assertTrue(response.references().isEmpty(), "no vector store under test => no references, proving the LLM branch was never reached");
        assertEquals(0, response.usedContextCount());
        assertNull(response.model(), "no LLM call was made, so model must be null");
        assertEquals(Answerability.NOT_FOUND, response.answerability());
        assertEquals("知识库未命中", response.answerabilityLabel());
        assertEquals(0.65, response.similarityThreshold(), 0.0001);
        assertEquals(0, response.rawHitCount());
        assertEquals(0, response.validHitCount());
    }

    @Test
    void moduleAndOperationTypeFiltersAreAcceptedWithoutBreakingTheCall() {
        assertDoesNotThrow(() -> aiRagAskService.ask(
                new AiRagAskRequest("库存有 100 件为什么不能出库？", "OUTBOUND", "OUTBOUND_LOCK", 3, 0.5)));
    }

    @Test
    void questionIsEchoedBackInResponse() {
        String question = "已发货出库单为什么不能取消？";
        AiRagAskResponse response = aiRagAskService.ask(new AiRagAskRequest(question, null, null, null, null));
        assertEquals(question, response.question());
    }

    @Test
    void noCurrentUserDefaultsToBusinessUserMode() {
        // This test calls the service directly (bypassing the HTTP/AuthInterceptor layer), so
        // CurrentUserContext is never populated — proving the "no role info => business user" default.
        assertNull(CurrentUserContext.get());
        AiRagAskResponse response = aiRagAskService.ask(new AiRagAskRequest("测试问题", null, null, null, null));
        assertEquals(RagAudienceMode.BUSINESS_USER, response.audienceMode());
        assertEquals("业务用户模式", response.audienceModeLabel());
    }

    @Test
    void technicalPromptExposesFieldNamesAndBusinessPromptDoesNot() {
        String technicalPrompt = aiRagAskService.systemPromptFor(RagAudienceMode.TECHNICAL_USER);
        String businessPrompt = aiRagAskService.systemPromptFor(RagAudienceMode.BUSINESS_USER);

        assertTrue(technicalPrompt.contains("on_hand_qty"), "technical mode must instruct the model it's allowed to use field names");
        assertTrue(technicalPrompt.contains("OUTBOUND_LOCK"));

        assertFalse(businessPrompt.contains("on_hand_qty"), "business mode's system prompt must not itself surface English field names");
        assertTrue(businessPrompt.contains("现存库存") || businessPrompt.contains("可用库存"),
                "business mode should instruct Chinese business terminology instead");
    }
}
