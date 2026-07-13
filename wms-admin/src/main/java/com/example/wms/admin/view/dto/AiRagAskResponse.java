package com.example.wms.admin.view.dto;

import java.util.List;

/**
 * {@code model} is null when no LLM call was made (no references found, or chat model unavailable).
 * {@code usedContextCount} may be less than {@code references.size()} when the 6000-char context
 * budget cut off trailing chunks; {@code references} always lists every retrieved hit regardless.
 */
public record AiRagAskResponse(
        String answer,
        String question,
        List<AiRagReferenceVO> references,
        Integer topK,
        int usedContextCount,
        String model
) {
}
