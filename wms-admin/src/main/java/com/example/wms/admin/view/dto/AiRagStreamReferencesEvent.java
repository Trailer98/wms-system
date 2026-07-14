package com.example.wms.admin.view.dto;

import java.util.List;

/** Payload of the SSE {@code references} event, sent once before any {@code delta}/{@code done}. */
public record AiRagStreamReferencesEvent(List<AiRagReferenceVO> references) {
}
