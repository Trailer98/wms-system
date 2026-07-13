package com.example.wms.admin.view.dto;

public record KnowledgeChunkView(
        Long id,
        String chunkCode,
        String title,
        String module,
        String sourceType,
        String operationType,
        String content,
        String contentHash,
        Integer sortOrder,
        String status,
        String vectorStatus,
        String vectorErrorMessage
) {
}
