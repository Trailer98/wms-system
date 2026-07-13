package com.example.wms.admin.controller.ai;

import com.example.wms.admin.annotation.RequiresPermission;
import com.example.wms.admin.annotation.SysOperationLog;
import com.example.wms.admin.service.AiRagAskService;
import com.example.wms.admin.view.dto.AiRagAskRequest;
import com.example.wms.admin.view.dto.AiRagAskResponse;
import com.example.wms.common.common.ApiResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** context-path is {@code /api}, so the effective path is {@code /api/ai/rag/ask}. */
@RestController
@RequestMapping("/ai/rag")
public class AiRagAskController {

    private final AiRagAskService aiRagAskService;

    public AiRagAskController(AiRagAskService aiRagAskService) {
        this.aiRagAskService = aiRagAskService;
    }

    @PostMapping("/ask")
    @RequiresPermission("ai-rag:ask")
    @SysOperationLog(operationType = "RAG知识问答", module = "AI问答", bizType = "AI_RAG_ASK")
    public ApiResponse<AiRagAskResponse> ask(@RequestBody AiRagAskRequest request) {
        return ApiResponse.ok(aiRagAskService.ask(request));
    }
}
