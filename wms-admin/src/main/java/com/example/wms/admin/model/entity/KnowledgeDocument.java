package com.example.wms.admin.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * AI knowledge base master document. The MySQL {@code knowledge_document} row is the source of truth;
 * pgvector only holds a vector index copy of the split {@link KnowledgeChunk}s. Table created in V4,
 * content + inherited metadata columns added in V5.
 */
@TableName("knowledge_document")
@Getter
@Setter
@NoArgsConstructor
public class KnowledgeDocument {

    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_ENABLED = "ENABLED";
    public static final String STATUS_DISABLED = "DISABLED";

    private Long id;
    private String docCode;
    private String title;
    private String module;
    private String sourceType;
    private String contentFormat;
    private String chunkStrategy;
    private String version;
    private String status;
    private String contentHash;
    /** Original body; chunker splits this into knowledge_chunk rows. */
    private String content;
    private String bizType;
    private String operationType;
    private String entityName;
    private String scenario;
    /** Comma-separated keywords, inherited by every chunk. */
    private String keywords;
    private String remark;
    private Long createdBy;
    private Long updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
