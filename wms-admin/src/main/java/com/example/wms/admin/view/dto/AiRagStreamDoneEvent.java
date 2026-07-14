package com.example.wms.admin.view.dto;

import com.example.wms.admin.service.RagAudienceMode;

/**
 * Payload of the terminal SSE {@code done} event: the full reassembled answer. Repeats
 * {@code audienceMode} (already sent in {@code meta}) so the frontend can calibrate against a single
 * final payload if it prefers.
 */
public record AiRagStreamDoneEvent(String answer, String model, int usedContextCount,
                                    RagAudienceMode audienceMode, String audienceModeLabel) {
}
