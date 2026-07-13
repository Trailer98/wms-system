package com.example.wms.admin.view.dto;

import com.example.wms.common.common.PageRequest;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KnowledgeChunkQuery extends PageRequest {

    private String status;
    private String vectorStatus;
}
