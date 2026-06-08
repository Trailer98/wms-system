package com.example.wms.admin.view.dto;

import com.example.wms.admin.model.entity.Inventory;

import java.time.Instant;

public record InventoryResponse(
        Long id,
        Long warehouseId,
        String warehouseCode,
        String warehouseName,
        Long skuId,
        String skuCode,
        String skuName,
        int quantity,
        int reservedQuantity,
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
                inventory.getQuantity(),
                inventory.getReservedQuantity(),
                inventory.getAvailableQuantity(),
                inventory.getUpdatedAt()
        );
    }
}
