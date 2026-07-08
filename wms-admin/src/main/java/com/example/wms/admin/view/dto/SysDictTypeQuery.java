package com.example.wms.admin.view.dto;

import com.example.wms.common.common.PageRequest;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SysDictTypeQuery extends PageRequest {

    private String dictCode;
    private String dictName;
    private String status;
}
