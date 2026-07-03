package com.example.wms.admin.view.dto;

import com.example.wms.admin.model.entity.Supplier;

import java.time.Instant;

public record SupplierResponse(
        Long id,
        String code,
        String name,
        String contactName,
        String contactPhone,
        String address,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {

    public static SupplierResponse from(Supplier supplier) {
        return new SupplierResponse(
                supplier.getId(),
                supplier.getCode(),
                supplier.getName(),
                supplier.getContactName(),
                supplier.getContactPhone(),
                supplier.getAddress(),
                supplier.isEnabled(),
                supplier.getCreatedAt(),
                supplier.getUpdatedAt()
        );
    }
}
