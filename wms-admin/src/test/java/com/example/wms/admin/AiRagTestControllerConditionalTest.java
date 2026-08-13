package com.example.wms.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.wms.admin.controller.ai.AiRagAskController;
import com.example.wms.admin.controller.ai.AiRagTestController;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Verifies {@link AiRagTestController} is gated by {@code wms.ai.test-endpoints-enabled} (default
 * {@code false}) on top of its existing {@code @Profile("ai")} gate, using Spring Boot's {@link
 * ApplicationContextRunner} — a lightweight condition-evaluation harness that registers just this one
 * controller (plus stub {@link VectorStore}/{@link ChatClient.Builder} beans so its constructor is
 * satisfiable) rather than booting the full application, which for this controller would otherwise
 * require a real Postgres pgvector datasource and a real DeepSeek-backed chat model.
 *
 * <p>No {@code @RequestMapping} handler method is ever registered for a bean that doesn't exist in the
 * context, so "bean absent/present" here is the correct, sufficient proxy for "route absent/present" —
 * this harness has no {@code DispatcherServlet} to assert HTTP routes against directly.
 */
class AiRagTestControllerConditionalTest {

    @Configuration
    static class StubAiBeansConfig {
        @Bean
        VectorStore vectorStore() {
            return mock(VectorStore.class);
        }

        @Bean
        ChatClient.Builder chatClientBuilder() {
            ChatClient.Builder builder = mock(ChatClient.Builder.class);
            when(builder.build()).thenReturn(mock(ChatClient.class));
            return builder;
        }
    }

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withInitializer(context -> context.getEnvironment().addActiveProfile("ai"))
            .withUserConfiguration(AiRagTestController.class, StubAiBeansConfig.class);

    // 默认情况下(未设置该属性) Bean 不存在 —— 同时证明仅激活 ai profile 本身不足以开启测试接口
    @Test
    void defaultConfig_aiProfileActiveWithoutProperty_beanAbsent() {
        contextRunner.run(context -> assertThat(context).doesNotHaveBean(AiRagTestController.class));
    }

    // 显式设为 false —— Bean 不存在
    @Test
    void explicitlyFalse_beanAbsent() {
        contextRunner
                .withPropertyValues("wms.ai.test-endpoints-enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(AiRagTestController.class));
    }

    // 显式设为 true(且 ai profile 已激活) —— Bean 存在
    @Test
    void explicitlyTrue_withAiProfile_beanPresent() {
        contextRunner
                .withPropertyValues("wms.ai.test-endpoints-enabled=true")
                .run(context -> assertThat(context).hasSingleBean(AiRagTestController.class));
    }

    // 显式设为 true 但 ai profile 未激活 —— Bean 仍不存在(两个门禁条件都要满足，@Profile 门禁不受本次改动影响)
    @Test
    void explicitlyTrue_withoutAiProfile_beanAbsent() {
        new ApplicationContextRunner()
                .withUserConfiguration(AiRagTestController.class, StubAiBeansConfig.class)
                .withPropertyValues("wms.ai.test-endpoints-enabled=true")
                .run(context -> assertThat(context).doesNotHaveBean(AiRagTestController.class));
    }

    // 正式 AiRagAskController 不依赖 ai profile、不依赖本次新增的开关，验证其类型本身未被改动影响
    // (真正的 HTTP/权限集成覆盖已经在既有的 AiRagAskControllerTest 里，这里只确认它不受本次条件门禁牵连)
    @Test
    void aiRagAskController_hasNoProfileOrConditionalConstraint() {
        assertThat(AiRagAskController.class.getAnnotation(org.springframework.context.annotation.Profile.class))
                .isNull();
        assertThat(AiRagAskController.class.getAnnotation(
                        org.springframework.boot.autoconfigure.condition.ConditionalOnProperty.class))
                .isNull();
    }
}
