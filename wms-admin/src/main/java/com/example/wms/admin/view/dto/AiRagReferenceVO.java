package com.example.wms.admin.view.dto;

/**
 * One knowledge chunk cited as a reference for a RAG answer. {@code contentPreview} is truncated.
 *
 * <p>{@code similarityScore} is exactly what {@link com.example.wms.admin.view.dto.KnowledgeSearchHit#score()}
 * carries — already a similarity (higher = more relevant), not a distance: Spring AI's
 * {@code PgVectorStore} computes {@code 1.0 - cosineDistance} internally before this value ever reaches
 * application code (see {@code AiRagAskService}'s class doc). Named explicitly {@code similarityScore}
 * here (rather than the ambiguous {@code score}) so no caller has to guess its direction.
 */
public record AiRagReferenceVO(
        Long chunkId,
        Long docId,
        String title,
        String module,
        String sourceType,
        String operationType,
        Double similarityScore,
        String contentPreview
) {
}
