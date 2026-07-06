package com.example.wms.admin.view.dto;

import com.example.wms.admin.model.entity.StockMovement;
import com.example.wms.common.enums.MovementType;

import java.time.Instant;

public record StockMovementResponse(
        Long id,
        MovementType type,
        String businessNo,
        Long skuId,
        String skuCode,
        String skuName,
        Long warehouseId,
        String warehouseCode,
        Long areaId,
        String areaCode,
        Long locationId,
        String locationCode,
        int quantityChange,
        Integer beforeQuantity,
        Integer afterQuantity,
        Integer beforeReservedQuantity,
        Integer afterReservedQuantity,
        Integer beforeFrozenQuantity,
        Integer afterFrozenQuantity,
        Integer beforeAvailableQuantity,
        Integer afterAvailableQuantity,
        String operator,
        Instant occurredAt,
        String remark
) {

    public static StockMovementResponse from(StockMovement movement) {
        return new StockMovementResponse(
                movement.getId(),
                movement.getType(),
                movement.getBusinessNo(),
                movement.getSku().getId(),
                movement.getSku().getCode(),
                movement.getSku().getName(),
                movement.getWarehouse().getId(),
                movement.getWarehouse().getCode(),
                movement.getArea() != null ? movement.getArea().getId() : null,
                movement.getArea() != null ? movement.getArea().getAreaCode() : null,
                movement.getLocation() != null ? movement.getLocation().getId() : null,
                movement.getLocation() != null ? movement.getLocation().getLocationCode() : null,
                movement.getQuantityChange(),
                movement.getBeforeQuantity(),
                movement.getAfterQuantity(),
                movement.getBeforeReservedQuantity(),
                movement.getAfterReservedQuantity(),
                movement.getBeforeFrozenQuantity(),
                movement.getAfterFrozenQuantity(),
                movement.getBeforeAvailableQuantity(),
                movement.getAfterAvailableQuantity(),
                movement.getOperator(),
                movement.getOccurredAt(),
                movement.getRemark()
        );
    }
}
