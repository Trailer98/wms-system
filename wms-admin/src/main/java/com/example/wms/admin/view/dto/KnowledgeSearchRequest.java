package com.example.wms.admin.view.dto;

/**
 * Knowledge retrieval test payload. {@code query} is required; {@code topK} defaults to 5 (max 20).
 * {@code module}/{@code operationType} are optional filters. Only searches the knowledge base — never
 * real-time inventory / orders / movements.
 */
public record KnowledgeSearchRequest(
        String query,
        String module,
        String operationType,
        Integer topK
) {
}
