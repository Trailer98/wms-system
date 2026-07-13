package com.example.wms.admin.view.dto;

import java.util.List;

public record KnowledgeSearchResponse(
        String query,
        List<KnowledgeSearchHit> records
) {
}
