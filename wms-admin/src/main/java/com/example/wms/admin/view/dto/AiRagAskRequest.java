package com.example.wms.admin.view.dto;

/**
 * RAG ask request. {@code question} is required; {@code module}/{@code operationType} are optional
 * metadata filters passed through to knowledge retrieval; {@code topK} defaults to 3 (max 8);
 * {@code temperature} defaults to 0.2.
 */
public record AiRagAskRequest(
        String question,
        String module,
        String operationType,
        Integer topK,
        Double temperature
) {
}
