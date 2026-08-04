package com.example.wms.admin.view.dto.base.customer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;


public record UpdateCustomerRequest (
        @NotNull Long id,
        @NotBlank @Size(max = 128) String name,
        @Size(max = 64) String contactName,
        @Size(max = 32) String contactPhone,
        @Size(max = 255) String address
) {
}
