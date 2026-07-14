package com.example.wms.admin.view.dto;

/** Payload of one SSE {@code delta} event: an incremental slice of the model's Markdown answer. */
public record AiRagStreamDeltaEvent(String text) {
}
