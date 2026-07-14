package com.example.wms.admin.view.dto;

/** Payload of the SSE {@code error} event. Sent instead of {@code done} when generation fails. */
public record AiRagStreamErrorEvent(String message) {
}
