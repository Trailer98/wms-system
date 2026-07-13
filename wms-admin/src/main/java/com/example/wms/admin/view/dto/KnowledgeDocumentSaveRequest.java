package com.example.wms.admin.view.dto;

import java.util.List;

/**
 * Create/update payload for a knowledge document. {@code keywords} is preferred as an array; the
 * service also tolerates a comma-separated string sent as a single-element list. When {@code docCode}
 * is blank the service generates one; when {@code autoVectorize} is true the service tries to embed the
 * chunks after saving (a failure is recorded on the chunk, it never fails the save).
 */
public record KnowledgeDocumentSaveRequest(
        String title,
        String docCode,
        String module,
        String sourceType,
        String bizType,
        String operationType,
        String entityName,
        String scenario,
        List<String> keywords,
        String version,
        String contentFormat,
        String chunkStrategy,
        Boolean autoVectorize,
        String status,
        String content
) {
}
