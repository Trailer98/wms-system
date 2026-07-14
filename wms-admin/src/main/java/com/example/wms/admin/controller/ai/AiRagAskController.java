package com.example.wms.admin.controller.ai;

import com.example.wms.admin.annotation.RequiresPermission;
import com.example.wms.admin.annotation.SysOperationLog;
import com.example.wms.admin.service.AiRagAskService;
import com.example.wms.admin.view.dto.AiRagAskRequest;
import com.example.wms.admin.view.dto.AiRagAskResponse;
import com.example.wms.common.common.ApiResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** context-path is {@code /api}, so the effective paths are {@code /api/ai/rag/ask} and {@code /api/ai/rag/ask/stream}. */
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

    /** Same request shape and RAG prompt/retrieval as {@link #ask}; streams the answer over SSE instead. */
    @PostMapping(value = "/ask/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @RequiresPermission("ai-rag:ask")
    @SysOperationLog(operationType = "RAG知识问答(流式)", module = "AI问答", bizType = "AI_RAG_ASK_STREAM")
    public SseEmitter askStream(@RequestBody AiRagAskRequest request) {
        return aiRagAskService.askStream(request);
    }
}
