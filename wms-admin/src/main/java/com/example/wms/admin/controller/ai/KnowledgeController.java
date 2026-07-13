package com.example.wms.admin.controller.ai;

import com.example.wms.admin.annotation.RequiresPermission;
import com.example.wms.admin.annotation.SysOperationLog;
import com.example.wms.admin.service.KnowledgeService;
import com.example.wms.admin.view.dto.KnowledgeChunkQuery;
import com.example.wms.admin.view.dto.KnowledgeChunkView;
import com.example.wms.admin.view.dto.KnowledgeDocumentDetail;
import com.example.wms.admin.view.dto.KnowledgeDocumentListItem;
import com.example.wms.admin.view.dto.KnowledgeDocumentQuery;
import com.example.wms.admin.view.dto.KnowledgeDocumentSaveRequest;
import com.example.wms.admin.view.dto.KnowledgeSaveResult;
import com.example.wms.admin.view.dto.KnowledgeSearchRequest;
import com.example.wms.admin.view.dto.KnowledgeSearchResponse;
import com.example.wms.admin.view.dto.KnowledgeStatusRequest;
import com.example.wms.admin.view.dto.KnowledgeVectorizeRequest;
import com.example.wms.admin.view.dto.KnowledgeVectorizeResult;
import com.example.wms.common.common.ApiResponse;
import com.example.wms.common.common.PageResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI knowledge base P0 endpoints. context-path is {@code /api}, so the effective prefix is
 * {@code /api/ai/knowledge}. MySQL is the master store; pgvector is only an index copy.
 */
@RestController
@RequestMapping("/ai/knowledge")
public class KnowledgeController {

    private static final String LOG_MODULE = "AI知识库";
    private static final String BIZ_TYPE = "AI_KNOWLEDGE_DOCUMENT";

    private final KnowledgeService knowledgeService;

    public KnowledgeController(KnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }

    @GetMapping("/documents/page")
    @RequiresPermission("ai-knowledge:view")
    public ApiResponse<PageResponse<KnowledgeDocumentListItem>> page(KnowledgeDocumentQuery query) {
        return ApiResponse.ok(knowledgeService.pageDocuments(query));
    }

    @GetMapping("/documents/{id}")
    @RequiresPermission("ai-knowledge:view")
    public ApiResponse<KnowledgeDocumentDetail> detail(@PathVariable Long id) {
        return ApiResponse.ok(knowledgeService.getDetail(id));
    }

    @GetMapping("/documents/{id}/chunks")
    @RequiresPermission("ai-knowledge:view")
    public ApiResponse<PageResponse<KnowledgeChunkView>> chunks(@PathVariable Long id, KnowledgeChunkQuery query) {
        return ApiResponse.ok(knowledgeService.pageChunks(id, query));
    }

    @PostMapping("/documents")
    @RequiresPermission("ai-knowledge:create")
    @SysOperationLog(operationType = "新增知识", module = LOG_MODULE, bizType = BIZ_TYPE,
            bizId = "#result.data.id", bizNo = "#result.data.docCode")
    public ApiResponse<KnowledgeSaveResult> create(@RequestBody KnowledgeDocumentSaveRequest request) {
        return ApiResponse.ok(knowledgeService.create(request));
    }

    @PutMapping("/documents/{id}")
    @RequiresPermission("ai-knowledge:update")
    @SysOperationLog(operationType = "编辑知识", module = LOG_MODULE, bizType = BIZ_TYPE,
            bizId = "#id", bizNo = "#result.data.docCode")
    public ApiResponse<KnowledgeSaveResult> update(@PathVariable Long id, @RequestBody KnowledgeDocumentSaveRequest request) {
        return ApiResponse.ok(knowledgeService.update(id, request));
    }

    @PatchMapping("/documents/{id}/status")
    @RequiresPermission("ai-knowledge:disable")
    @SysOperationLog(operationType = "修改知识状态", module = LOG_MODULE, bizType = BIZ_TYPE,
            bizId = "#id", bizNo = "#result.data.docCode")
    public ApiResponse<KnowledgeDocumentDetail> changeStatus(@PathVariable Long id, @RequestBody KnowledgeStatusRequest request) {
        return ApiResponse.ok(knowledgeService.changeStatus(id, request.status()));
    }

    @PostMapping("/documents/{id}/vectorize")
    @RequiresPermission("ai-knowledge:vectorize")
    @SysOperationLog(operationType = "重新向量化", module = LOG_MODULE, bizType = BIZ_TYPE, bizId = "#id")
    public ApiResponse<KnowledgeVectorizeResult> vectorize(@PathVariable Long id, @RequestBody(required = false) KnowledgeVectorizeRequest request) {
        boolean force = request != null && Boolean.TRUE.equals(request.force());
        return ApiResponse.ok(knowledgeService.vectorize(id, force));
    }

    @PostMapping("/search")
    @RequiresPermission("ai-knowledge:search")
    public ApiResponse<KnowledgeSearchResponse> search(@RequestBody KnowledgeSearchRequest request) {
        return ApiResponse.ok(knowledgeService.search(request));
    }
}
