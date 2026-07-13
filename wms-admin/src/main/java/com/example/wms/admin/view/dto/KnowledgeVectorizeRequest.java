package com.example.wms.admin.view.dto;

/**
 * {@code force=false}: skip when the document content hash is unchanged. {@code force=true}: re-chunk
 * and re-embed regardless. Never mutates the original document content.
 */
public record KnowledgeVectorizeRequest(Boolean force) {
}
