package com.example.wms.admin.view.dto;

import com.example.wms.admin.model.entity.Sku;

import java.time.Instant;

public record SkuResponse(
        Long id,
        String code,
        String name,
        String unit,
        String category,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {

    public static SkuResponse from(Sku sku) {
        return new SkuResponse(
                sku.getId(),
                sku.getCode(),
                sku.getName(),
                sku.getUnit(),
                sku.getCategory(),
                sku.isEnabled(),
                sku.getCreatedAt(),
                sku.getUpdatedAt()
        );
    }
}
