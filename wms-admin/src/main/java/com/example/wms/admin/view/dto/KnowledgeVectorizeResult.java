package com.example.wms.admin.view.dto;

public record KnowledgeVectorizeResult(
        int successCount,
        int failedCount,
        int skipped,
        String message
) {
}
