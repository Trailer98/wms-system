package com.example.wms.admin.view.dto;

import com.example.wms.admin.model.entity.Customer;

import java.time.Instant;

public record CustomerResponse(
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

    public static CustomerResponse from(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getCode(),
                customer.getName(),
                customer.getContactName(),
                customer.getContactPhone(),
                customer.getAddress(),
                customer.isEnabled(),
                customer.getCreatedAt(),
                customer.getUpdatedAt()
        );
    }
}
