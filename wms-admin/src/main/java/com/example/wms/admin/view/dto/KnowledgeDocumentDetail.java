package com.example.wms.admin.view.dto;

import java.time.LocalDateTime;
import java.util.List;

/** Full knowledge document, including the original {@code content} and parsed {@code keywords}. */
public record KnowledgeDocumentDetail(
        Long id,
        String docCode,
        String title,
        String module,
        String moduleLabel,
        String sourceType,
        String sourceTypeLabel,
        String bizType,
        String operationType,
        String entityName,
        String scenario,
        List<String> keywords,
        String version,
        String contentFormat,
        String chunkStrategy,
        String status,
        String content,
        long chunkCount,
        long indexedChunkCount,
        long failedChunkCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
