package com.example.wms.admin.view.dto;

import com.example.wms.admin.model.entity.WarehouseArea;
import com.example.wms.common.enums.AreaStatus;
import com.example.wms.common.enums.AreaType;

import java.time.Instant;

public record WarehouseAreaResponse(
        Long id,
        Long warehouseId,
        String warehouseCode,
        String warehouseName,
        String areaCode,
        String areaName,
        AreaType areaType,
        AreaStatus status,
        int pickPriority,
        String remark,
        Instant createdAt,
        Instant updatedAt
) {

    public static WarehouseAreaResponse from(WarehouseArea area) {
        return new WarehouseAreaResponse(
                area.getId(),
                area.getWarehouse().getId(),
                area.getWarehouse().getCode(),
                area.getWarehouse().getName(),
                area.getAreaCode(),
                area.getAreaName(),
                area.getAreaType(),
                area.getStatus(),
                area.getPickPriority(),
                area.getRemark(),
                area.getCreatedAt(),
                area.getUpdatedAt()
        );
    }
}
