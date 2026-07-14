package com.example.wms.admin.view.dto;

import com.example.wms.admin.service.Answerability;
import com.example.wms.admin.service.RagAudienceMode;

/**
 * Payload of the SSE {@code meta} event: sent once retrieval + similarity-threshold filtering has
 * finished (so it can carry {@code rawHitCount}/{@code validHitCount}/{@code answerability}), and
 * always before {@code references}/{@code delta}/{@code done}.
 */
public record AiRagStreamMetaEvent(
        RagAudienceMode audienceMode,
        String audienceModeLabel,
        String model,
        Answerability answerability,
        String answerabilityLabel,
        Integer topK,
        Double similarityThreshold,
        int rawHitCount,
        int validHitCount
) {
}
