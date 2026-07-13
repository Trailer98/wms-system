package com.example.wms.admin.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * A split piece of a {@link KnowledgeDocument}. Metadata (module/sourceType/bizType/operationType/
 * entityName/scenario/keywords) is inherited from the parent document at chunk time. vector_status
 * tracks the pgvector index lifecycle and never blocks the knowledge-management main flow. Table
 * created in V4.
 */
@TableName("knowledge_chunk")
@Getter
@Setter
@NoArgsConstructor
public class KnowledgeChunk {

    public static final String STATUS_ENABLED = "ENABLED";
    public static final String STATUS_DISABLED = "DISABLED";

    public static final String VECTOR_PENDING = "PENDING";
    public static final String VECTOR_INDEXED = "INDEXED";
    public static final String VECTOR_FAILED = "FAILED";
    public static final String VECTOR_STALE = "STALE";

    private Long id;
    private Long docId;
    private String chunkCode;
    private String title;
    private String module;
    private String sourceType;
    private String bizType;
    private String operationType;
    private String entityName;
    private String scenario;
    private String keywords;
    private String content;
    private String contentHash;
    private Integer sortOrder;
    private String status;
    private String vectorStatus;
    private String vectorErrorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
