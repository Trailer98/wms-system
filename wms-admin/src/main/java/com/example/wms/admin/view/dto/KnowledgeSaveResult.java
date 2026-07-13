package com.example.wms.admin.view.dto;

public record KnowledgeSaveResult(
        Long id,
        String docCode,
        int chunkCount,
        boolean vectorized
) {
}
