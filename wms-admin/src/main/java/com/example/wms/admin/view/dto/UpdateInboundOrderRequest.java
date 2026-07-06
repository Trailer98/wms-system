package com.example.wms.admin.view.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record UpdateInboundOrderRequest(
        Long supplierId,
        @Valid @NotEmpty List<InboundOrderItemRequest> items
) {
}
