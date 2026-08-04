package com.example.wms.admin.view.dto.base.customer;

import com.example.wms.common.common.PageRequest;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomerQuery extends PageRequest {
    private Boolean enabled;
    private String code;
    private String name;
}
