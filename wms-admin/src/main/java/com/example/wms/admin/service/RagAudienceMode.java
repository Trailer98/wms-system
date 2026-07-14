package com.example.wms.admin.service;

/**
 * Which style of RAG answer to generate. Decided server-side from the current user's roles only (see
 * {@link AiRagAskService}) — never from client-supplied input — so a caller can't self-elevate to the
 * technical mode by sending a request field.
 */
public enum RagAudienceMode {
    BUSINESS_USER("业务用户模式"),
    TECHNICAL_USER("技术人员模式");

    private final String label;

    RagAudienceMode(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
