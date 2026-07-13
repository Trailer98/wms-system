package com.example.wms.admin.controller.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Manual RAG smoke-test endpoints. Gated on the {@code ai} profile: it depends on the {@link VectorStore}
 * / {@link ChatClient} beans that only exist under {@code ai} (see {@code AiVectorStoreConfig}), so
 * without this the application context fails to load under other profiles (e.g. {@code test}).
 */
@RestController
@RequestMapping("/ai/test")
@Profile("ai")
public class AiRagTestController {

    private final VectorStore vectorStore;
    private final ChatClient chatClient;

    public AiRagTestController(VectorStore vectorStore,
                               ChatClient.Builder chatClientBuilder) {
        this.vectorStore = vectorStore;
        this.chatClient = chatClientBuilder.build();
    }

    /**
     * 第一步：写入测试知识库文档。
     *
     * 执行后会发生：
     * 1. Spring Boot 调用 vectorStore.add()
     * 2. Spring AI 调用 Ollama bge-m3 生成 embedding
     * 3. PgVectorStore 写入 PostgreSQL vector_store 表
     */
    @PostMapping("/add")
    public Map<String, Object> add() {
        List<Document> documents = List.of(
                new Document(
                        "WMS 中可用库存 = 总库存 - 占用库存 - 冻结库存。",
                        Map.of(
                                "module", "inventory",
                                "type", "rule",
                                "source", "manual-test"
                        )
                ),
                new Document(
                        "出库单创建时，需要校验 SKU 是否存在、仓库是否存在、可用库存是否充足。",
                        Map.of(
                                "module", "outbound",
                                "type", "rule",
                                "source", "manual-test"
                        )
                ),
                new Document(
                        "库存冻结通常发生在盘点、质检、异常处理等场景。",
                        Map.of(
                                "module", "inventory",
                                "type", "rule",
                                "source", "manual-test"
                        )
                ),
                new Document(
                        "WMS 查询某个 SKU 是否可以出库时，应优先查看可用库存，而不是只看总库存。",
                        Map.of(
                                "module", "inventory",
                                "type", "faq",
                                "source", "manual-test"
                        )
                )
        );

        vectorStore.add(documents);

        return Map.of(
                "success", true,
                "inserted", documents.size()
        );
    }

    /**
     * 第二步：只做相似度检索，不调用 DeepSeek。
     *
     * 执行后会发生：
     * 1. 用户问题通过 Ollama bge-m3 生成 query embedding
     * 2. PgVectorStore 在 PostgreSQL 中做 similaritySearch
     * 3. 返回最相似的文档
     */
    @GetMapping("/search")
    public List<Map<String, Object>> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "3") Integer topK
    ) {
        List<Document> results = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(q)
                        .topK(topK)
                        .build()
        );

        return results.stream()
                .map(document -> Map.<String, Object>of(
                        "id", document.getId(),
                        "text", document.getText(),
                        "metadata", document.getMetadata(),
                        "score", document.getScore()
                ))
                .toList();
    }

    /**
     * 第三步：RAG 问答。
     *
     * 执行后会发生：
     * 1. similaritySearch 召回知识库内容
     * 2. 把召回内容拼成上下文
     * 3. 调用 DeepSeek 生成最终回答
     */
    @GetMapping("/ask")
    public Map<String, Object> ask(@RequestParam String q) {
        List<Document> docs = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(q)
                        .topK(3)
                        .build()
        );

        String context = docs.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n"));

        String answer = chatClient.prompt()
                .system("""
                        你是 WMS 系统 AI 助手。
                        你只能根据给定的知识库内容回答。
                        如果知识库内容不足，不要编造，回答：当前知识库中没有足够信息。
                        回答要简洁、准确、偏业务解释。
                        """)
                .user("""
                        知识库内容：
                        %s

                        用户问题：
                        %s
                        """.formatted(context, q))
                .call()
                .content();

        return Map.of(
                "question", q,
                "retrievedDocs", docs.stream()
                        .map(document -> Map.<String, Object>of(
                                "text", document.getText(),
                                "metadata", document.getMetadata(),
                                "score", document.getScore()
                        ))
                        .toList(),
                "answer", answer
        );
    }
}