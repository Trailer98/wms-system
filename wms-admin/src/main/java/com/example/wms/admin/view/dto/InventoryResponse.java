package com.example.wms.admin.view.dto;

import com.example.wms.admin.model.entity.Inventory;
import com.example.wms.common.enums.AreaType;
import com.example.wms.common.enums.InventoryStatus;

import java.time.Instant;

public record InventoryResponse(
        Long id,
        Long warehouseId,
        String warehouseCode,
        String warehouseName,
        Long skuId,
        String skuCode,
        String skuName,
        Long areaId,
        String areaCode,
        String areaName,
        AreaType areaType,
        Long locationId,
        String locationCode,
        String locationName,
        InventoryStatus inventoryStatus,
        int quantity,
        int reservedQuantity,
        int frozenQuantity,
        int availableQuantity,
        Instant updatedAt
) {

    public static InventoryResponse from(Inventory inventory) {
        return new InventoryResponse(
                inventory.getId(),
                inventory.getWarehouse().getId(),
                inventory.getWarehouse().getCode(),
                inventory.getWarehouse().getName(),
                inventory.getSku().getId(),
                inventory.getSku().getCode(),
                inventory.getSku().getName(),
                inventory.getArea() != null ? inventory.getArea().getId() : null,
                inventory.getArea() != null ? inventory.getArea().getAreaCode() : null,
                inventory.getArea() != null ? inventory.getArea().getAreaName() : null,
                inventory.getArea() != null ? inventory.getArea().getAreaType() : null,
                inventory.getLocation() != null ? inventory.getLocation().getId() : null,
                inventory.getLocation() != null ? inventory.getLocation().getLocationCode() : null,
                inventory.getLocation() != null ? inventory.getLocation().getLocationName() : null,
                inventory.getInventoryStatus(),
                inventory.getQuantity(),
                inventory.getReservedQuantity(),
                inventory.getFrozenQuantity(),
                inventory.getAvailableQuantity(),
                inventory.getUpdatedAt()
        );
    }
}
