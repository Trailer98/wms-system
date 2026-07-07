package com.example.wms.admin.view.dto;

import com.example.wms.common.common.PageRequest;
import com.example.wms.common.enums.CommonStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserQuery extends PageRequest {

    private String username;
    private CommonStatus status;
}
