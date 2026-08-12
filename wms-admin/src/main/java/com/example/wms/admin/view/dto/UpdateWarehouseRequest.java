package com.example.wms.admin.view.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateWarehouseRequest(
        @NotNull Long id,
        @NotBlank @Size(max = 128) String name,
        @Size(max = 255) String address
) {
}
