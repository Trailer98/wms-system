package com.example.wms.admin.view.dto;

import com.example.wms.common.common.PageRequest;
import com.example.wms.common.enums.AreaStatus;
import com.example.wms.common.enums.AreaType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WarehouseAreaQuery extends PageRequest {

    private Long warehouseId;
    private String areaCode;
    private String areaName;
    private AreaType areaType;
    private AreaStatus status;
}
