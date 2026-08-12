package com.example.wms.admin.view.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateWarehouseEnabledRequest(
        @NotNull Long id,
        @NotNull boolean enabled
) {
}
