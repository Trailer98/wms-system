package com.example.wms.admin.view.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record InboundOrderItemRequest(
        @NotNull Long skuId,
        @Positive int quantity,
        @NotNull Long areaId,
        @NotNull Long locationId
) {
}
