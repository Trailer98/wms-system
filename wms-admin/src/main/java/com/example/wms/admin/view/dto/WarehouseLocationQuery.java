package com.example.wms.admin.view.dto;

import com.example.wms.common.common.PageRequest;
import com.example.wms.common.enums.LocationStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WarehouseLocationQuery extends PageRequest {

    private Long warehouseId;
    private Long areaId;
    private String locationCode;
    private LocationStatus status;
}
