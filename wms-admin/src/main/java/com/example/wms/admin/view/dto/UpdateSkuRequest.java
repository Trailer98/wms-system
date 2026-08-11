package com.example.wms.admin.view.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateSkuRequest(
        @NotNull Long id,
        @NotBlank @Size(max = 128) String name,
        @NotBlank @Size(max = 32) String unit,
        @Size(max = 64) String category
) {
}
