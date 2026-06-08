package com.example.wms.admin.view.dto;

import com.example.wms.admin.model.entity.Warehouse;

import java.time.Instant;

public record WarehouseResponse(
        Long id,
        String code,
        String name,
        String address,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {

    public static WarehouseResponse from(Warehouse warehouse) {
        return new WarehouseResponse(
                warehouse.getId(),
                warehouse.getCode(),
                warehouse.getName(),
                warehouse.getAddress(),
                warehouse.isEnabled(),
                warehouse.getCreatedAt(),
                warehouse.getUpdatedAt()
        );
    }
}
