package com.example.wms.admin.view.dto;

import com.example.wms.admin.service.Answerability;
import com.example.wms.admin.service.RagAudienceMode;

import java.util.List;

/**
 * {@code model} is null when no LLM call was made (no valid hits, or chat model unavailable).
 * {@code references} only ever contains hits whose similarityScore reached
 * {@code similarityThreshold} ("validHits") — a hit below threshold is never surfaced here, never fed
 * into the prompt, and never counted toward {@code usedContextCount}. {@code rawHitCount} is how many
 * candidates the vector search returned before that filter; {@code validHitCount} is how many survived
 * it (equal to {@code references.size()}). {@code audienceMode} is decided server-side from the
 * caller's roles, never from client input.
 */
public record AiRagAskResponse(
        String answer,
        String question,
        List<AiRagReferenceVO> references,
        Integer topK,
        int usedContextCount,
        String model,
        RagAudienceMode audienceMode,
        String audienceModeLabel,
        Answerability answerability,
        String answerabilityLabel,
        Double similarityThreshold,
        int rawHitCount,
        int validHitCount
) {
}
