package com.example.wms.admin.view.dto;

import com.example.wms.admin.model.entity.WarehouseLocation;
import com.example.wms.common.enums.LocationStatus;
import com.example.wms.common.enums.LocationType;

import java.time.Instant;

public record WarehouseLocationResponse(
        Long id,
        Long warehouseId,
        String warehouseCode,
        String warehouseName,
        Long areaId,
        String areaCode,
        String areaName,
        String locationCode,
        String locationName,
        LocationType locationType,
        LocationStatus status,
        int capacityQty,
        int usedQty,
        boolean allowMixedSku,
        int pickPriority,
        String remark,
        Instant createdAt,
        Instant updatedAt
) {

    public static WarehouseLocationResponse from(WarehouseLocation location) {
        return new WarehouseLocationResponse(
                location.getId(),
                location.getWarehouse().getId(),
                location.getWarehouse().getCode(),
                location.getWarehouse().getName(),
                location.getArea().getId(),
                location.getArea().getAreaCode(),
                location.getArea().getAreaName(),
                location.getLocationCode(),
                location.getLocationName(),
                location.getLocationType(),
                location.getStatus(),
                location.getCapacityQty(),
                location.getUsedQty(),
                location.isAllowMixedSku(),
                location.getPickPriority(),
                location.getRemark(),
                location.getCreatedAt(),
                location.getUpdatedAt()
        );
    }
}
