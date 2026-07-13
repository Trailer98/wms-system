package com.example.wms.admin.view.dto;

public record KnowledgeSearchHit(
        Long chunkId,
        Long docId,
        String title,
        String module,
        String sourceType,
        String operationType,
        Double score,
        String content
) {
}
