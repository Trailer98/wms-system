package com.example.wms.admin.view.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateOutboundOrderRequest(
        @NotBlank @Size(max = 64) String orderNo,
        @NotNull Long warehouseId,
        @Size(max = 128) String customerName,
        @Valid @NotEmpty List<OrderItemRequest> items
) {
}
