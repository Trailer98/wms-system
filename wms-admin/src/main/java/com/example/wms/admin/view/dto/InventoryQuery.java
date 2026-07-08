package com.example.wms.admin.view.dto;

import com.example.wms.common.common.PageRequest;
import com.example.wms.common.enums.InventoryStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InventoryQuery extends PageRequest {

    private Long warehouseId;
    private Long skuId;
    private String skuCode;
    private Long areaId;
    private Long locationId;
    private InventoryStatus inventoryStatus;
    private Boolean hasStock;
    private Boolean onlyAvailable;
}
