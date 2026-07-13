package com.example.wms.admin.view.dto;

/** One knowledge chunk cited as a reference for a RAG answer. {@code contentPreview} is truncated. */
public record AiRagReferenceVO(
        Long chunkId,
        Long docId,
        String title,
        String module,
        String sourceType,
        String operationType,
        Double score,
        String contentPreview
) {
}
