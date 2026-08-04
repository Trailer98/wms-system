package com.example.wms.admin.view.dto.base.customer;

import jakarta.validation.constraints.NotNull;

public record UpdateEnabledRequest(
        @NotNull Long id,
        @NotNull boolean enabled
) {
}
