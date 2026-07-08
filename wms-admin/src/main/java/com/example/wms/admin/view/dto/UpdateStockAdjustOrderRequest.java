package com.example.wms.admin.view.dto;

import com.example.wms.common.enums.AdjustReasonType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record UpdateStockAdjustOrderRequest(
        @NotNull AdjustReasonType reasonType,
        String reason,
        @Valid @NotEmpty List<StockAdjustOrderItemRequest> items
) {
}
