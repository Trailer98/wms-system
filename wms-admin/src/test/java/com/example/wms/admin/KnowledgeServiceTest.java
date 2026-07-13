package com.example.wms.admin;

import com.example.wms.admin.model.entity.KnowledgeChunk;
import com.example.wms.admin.service.KnowledgeService;
import com.example.wms.admin.view.dto.KnowledgeChunkQuery;
import com.example.wms.admin.view.dto.KnowledgeChunkView;
import com.example.wms.admin.view.dto.KnowledgeDocumentDetail;
import com.example.wms.admin.view.dto.KnowledgeDocumentQuery;
import com.example.wms.admin.view.dto.KnowledgeDocumentSaveRequest;
import com.example.wms.admin.view.dto.KnowledgeSaveResult;
import com.example.wms.admin.view.dto.KnowledgeSearchRequest;
import com.example.wms.admin.view.dto.KnowledgeVectorizeResult;
import com.example.wms.common.common.BusinessException;
import com.example.wms.common.common.PageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs under the {@code test} profile where the {@code ai} VectorStore bean is absent, so this verifies
 * the knowledge-management main flow (chunking / persistence / status) works with the vector link
 * unavailable, and that vectorize/search degrade with a clear signal instead of faking success.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:mysql://localhost:3306/wms_system_test?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Tokyo&useSSL=false&allowPublicKeyRetrieval=true")
class KnowledgeServiceTest {

    @Autowired
    private KnowledgeService knowledgeService;

    private String suffix;

    @BeforeEach
    void setUp() {
        suffix = String.valueOf(System.nanoTime());
    }

    private KnowledgeDocumentSaveRequest request(String strategy, String content, boolean autoVectorize, String status) {
        return new KnowledgeDocumentSaveRequest(
                "出库锁库库存校验规则-" + suffix,
                "OUTBOUND_LOCK_RULE_" + suffix,
                "OUTBOUND",
                "RULE",
                "OUTBOUND_ORDER",
                "OUTBOUND_LOCK",
                "outbound_order",
                "出库锁库时校验可用库存",
                List.of("出库", "锁库", "可用库存"),
                "v1.0",
                "MARKDOWN",
                strategy,
                autoVectorize,
                status,
                content
        );
    }

    private String longSection(String heading, String seed) {
        return "## " + heading + "\n\n" + seed.repeat(160) + "\n\n";
    }

    @Test
    void createSemanticMarkdownSplitsByHeadings() {
        String content = longSection("锁库前置校验", "库存")
                + longSection("锁库失败处理", "异常")
                + longSection("锁库成功后动作", "台账");
        KnowledgeSaveResult result = knowledgeService.create(request("SEMANTIC_MARKDOWN", content, false, "ENABLED"));

        assertNotNull(result.id());
        assertTrue(result.chunkCount() >= 2, "H2 sections should yield multiple chunks, got " + result.chunkCount());
        assertFalse(result.vectorized(), "no vector store under test profile, so not vectorized");

        KnowledgeDocumentDetail detail = knowledgeService.getDetail(result.id());
        assertEquals(content, detail.content(), "original content is preserved on the document");
        assertEquals(List.of("出库", "锁库", "可用库存"), detail.keywords());
        assertEquals("出库管理", detail.moduleLabel());
        assertEquals("业务规则", detail.sourceTypeLabel());
    }

    @Test
    void createNoneStrategyProducesSingleChunk() {
        KnowledgeSaveResult result = knowledgeService.create(
                request("NONE", "可用库存 = 总库存 - 占用库存 - 冻结库存。", false, "ENABLED"));
        assertEquals(1, result.chunkCount());
    }

    @Test
    void createFixedSizeSplitsLongContent() {
        String content = "出库锁库".repeat(600); // 2400 chars, well over the 800-char window
        KnowledgeSaveResult result = knowledgeService.create(request("FIXED_SIZE", content, false, "ENABLED"));
        assertTrue(result.chunkCount() >= 2, "fixed-size should window long content, got " + result.chunkCount());
    }

    @Test
    void chunksInheritDocumentMetadata() {
        KnowledgeSaveResult result = knowledgeService.create(
                request("NONE", "锁库时校验可用库存。", false, "ENABLED"));
        KnowledgeChunkView chunk = knowledgeService.pageChunks(result.id(), new KnowledgeChunkQuery()).records().get(0);
        assertEquals("OUTBOUND", chunk.module());
        assertEquals("RULE", chunk.sourceType());
        assertEquals("OUTBOUND_LOCK", chunk.operationType());
        assertEquals(KnowledgeChunk.VECTOR_PENDING, chunk.vectorStatus());
    }

    @Test
    void editContentReplacesChunksWithoutLeftovers() {
        String content = longSection("A 段", "甲") + longSection("B 段", "乙") + longSection("C 段", "丙");
        KnowledgeSaveResult created = knowledgeService.create(request("SEMANTIC_MARKDOWN", content, false, "ENABLED"));
        assertTrue(created.chunkCount() >= 2);

        // Re-edit to a single short NONE chunk: old chunks must be gone, not duplicated.
        KnowledgeDocumentSaveRequest edit = new KnowledgeDocumentSaveRequest(
                "出库锁库库存校验规则-" + suffix, created.docCode(), "OUTBOUND", "RULE",
                "OUTBOUND_ORDER", "OUTBOUND_LOCK", "outbound_order", "编辑后场景",
                List.of("锁库"), "v1.1", "MARKDOWN", "NONE", false, "ENABLED", "编辑后的简短规则内容。");
        KnowledgeSaveResult updated = knowledgeService.update(created.id(), edit);

        assertEquals(1, updated.chunkCount());
        PageResponse<KnowledgeChunkView> chunks = knowledgeService.pageChunks(created.id(), new KnowledgeChunkQuery());
        assertEquals(1, chunks.total(), "editing content must not leave stale chunks behind");
    }

    @Test
    void disableDocumentDisablesChunksAndHidesFromSearchEligibility() {
        KnowledgeSaveResult created = knowledgeService.create(
                request("NONE", "停用后不应参与检索。", false, "ENABLED"));
        knowledgeService.changeStatus(created.id(), "DISABLED");

        KnowledgeDocumentDetail detail = knowledgeService.getDetail(created.id());
        assertEquals("DISABLED", detail.status());
        KnowledgeChunkView chunk = knowledgeService.pageChunks(created.id(), new KnowledgeChunkQuery()).records().get(0);
        assertEquals(KnowledgeChunk.STATUS_DISABLED, chunk.status(), "chunks mirror the document status");
    }

    @Test
    void vectorizeIsCallableAndRecordsFailedWhenVectorStoreUnavailable() {
        KnowledgeSaveResult created = knowledgeService.create(
                request("NONE", "重新向量化接口应可调用。", false, "ENABLED"));
        KnowledgeVectorizeResult result = knowledgeService.vectorize(created.id(), true);

        assertEquals(0, result.successCount());
        assertEquals(1, result.failedCount(), "no vector store under test → chunk recorded FAILED, not faked");
        assertNotNull(result.message());

        KnowledgeChunkView chunk = knowledgeService.pageChunks(created.id(), new KnowledgeChunkQuery()).records().get(0);
        assertEquals(KnowledgeChunk.VECTOR_FAILED, chunk.vectorStatus());
        assertNotNull(chunk.vectorErrorMessage());
    }

    @Test
    void searchFailsClearlyWhenVectorLinkUnavailable() {
        // Must surface a clear error rather than pretend success when pgvector isn't wired.
        assertThrows(BusinessException.class,
                () -> knowledgeService.search(new KnowledgeSearchRequest("为什么锁库后库存没有减少？", "OUTBOUND", null, 5)));
    }

    @Test
    void searchRejectsBlankQuery() {
        assertThrows(BusinessException.class,
                () -> knowledgeService.search(new KnowledgeSearchRequest("  ", null, null, 5)));
    }

    @Test
    void pageReturnsCreatedDocumentWithChunkCounts() {
        KnowledgeSaveResult created = knowledgeService.create(
                request("NONE", "分页应能查询到该文档。", false, "ENABLED"));
        KnowledgeDocumentQuery query = new KnowledgeDocumentQuery();
        query.setKeyword(created.docCode());
        PageResponse<?> page = knowledgeService.pageDocuments(query);
        assertTrue(page.total() >= 1);
    }
}
