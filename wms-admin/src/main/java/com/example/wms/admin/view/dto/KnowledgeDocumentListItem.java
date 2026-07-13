package com.example.wms.admin.view.dto;

import java.time.LocalDateTime;

/** One row of the knowledge document page. Deliberately excludes the large {@code content} field. */
public record KnowledgeDocumentListItem(
        Long id,
        String docCode,
        String title,
        String module,
        String moduleLabel,
        String sourceType,
        String sourceTypeLabel,
        String contentFormat,
        String chunkStrategy,
        String version,
        String status,
        long chunkCount,
        long indexedChunkCount,
        long failedChunkCount,
        LocalDateTime updatedAt
) {
}
