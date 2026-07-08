package com.example.wms.admin.view.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** {@code orderNo} is accepted for backward compatibility but ignored: the system always mints its own number. */
public record CreateInboundOrderRequest(
        @Size(max = 64) String orderNo,
        @NotNull Long warehouseId,
        Long supplierId,
        @Valid @NotEmpty List<InboundOrderItemRequest> items
) {
}
