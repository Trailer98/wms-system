package com.example.wms.admin.view.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record StockCountItemActualRequest(
        @NotNull Long itemId,
        @PositiveOrZero int actualQty,
        String remark
) {
}
