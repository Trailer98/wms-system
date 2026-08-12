package com.example.wms.admin.view.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateSupplierEnabledRequest(
        @NotNull Long id,
        @NotNull boolean enabled
) {
}
