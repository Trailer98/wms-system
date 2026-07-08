package com.example.wms.admin.view.dto;

import com.example.wms.admin.model.entity.StockAdjustOrder;
import com.example.wms.common.enums.AdjustReasonType;
import com.example.wms.common.enums.StockAdjustOrderStatus;

import java.time.Instant;
import java.util.List;

public record StockAdjustOrderResponse(
        Long id,
        String adjustNo,
        StockAdjustOrderStatus status,
        AdjustReasonType reasonType,
        String reason,
        Long warehouseId,
        String warehouseCode,
        List<StockAdjustOrderItemResponse> items,
        String createdBy,
        String confirmedBy,
        Instant confirmedAt,
        String cancelledBy,
        Instant cancelledAt,
        String cancelReason,
        Instant createdAt,
        Instant updatedAt
) {

    public static StockAdjustOrderResponse from(StockAdjustOrder order) {
        return new StockAdjustOrderResponse(
                order.getId(),
                order.getAdjustNo(),
                order.getStatus(),
                order.getReasonType(),
                order.getReason(),
                order.getWarehouse().getId(),
                order.getWarehouse().getCode(),
                order.getItems().stream().map(StockAdjustOrderItemResponse::from).toList(),
                order.getCreatedBy(),
                order.getConfirmedBy(),
                order.getConfirmedAt(),
                order.getCancelledBy(),
                order.getCancelledAt(),
                order.getCancelReason(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
