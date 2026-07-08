package com.example.wms.admin.view.dto;

import com.example.wms.common.enums.AdjustReasonType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** {@code adjustNo} is accepted for backward compatibility but ignored: the system always mints its own number. */
public record CreateStockAdjustOrderRequest(
        @Size(max = 64) String adjustNo,
        @NotNull Long warehouseId,
        @NotNull AdjustReasonType reasonType,
        String reason,
        @Valid @NotEmpty List<StockAdjustOrderItemRequest> items
) {
}
