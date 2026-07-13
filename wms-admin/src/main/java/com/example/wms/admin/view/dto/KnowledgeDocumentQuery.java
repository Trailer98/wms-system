package com.example.wms.admin.view.dto;

import com.example.wms.common.common.PageRequest;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KnowledgeDocumentQuery extends PageRequest {

    private String title;
    private String module;
    private String sourceType;
    private String status;
    /** Free-text match on title/docCode/keywords. */
    private String keyword;
    private String vectorStatus;
}
