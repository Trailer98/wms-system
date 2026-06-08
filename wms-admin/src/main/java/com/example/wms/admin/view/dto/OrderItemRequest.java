package com.example.wms.admin.view.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OrderItemRequest(
        @NotNull Long skuId,
        @Positive int quantity
) {
}
