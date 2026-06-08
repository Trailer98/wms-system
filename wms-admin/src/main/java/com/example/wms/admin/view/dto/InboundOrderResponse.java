package com.example.wms.admin.view.dto;

import com.example.wms.admin.model.entity.InboundOrder;
import com.example.wms.common.enums.InboundOrderStatus;

import java.time.Instant;
import java.util.List;

public record InboundOrderResponse(
        Long id,
        String orderNo,
        InboundOrderStatus status,
        Long warehouseId,
        String warehouseCode,
        String supplierName,
        List<OrderItemResponse> items,
        Instant receivedAt,
        Instant createdAt,
        Instant updatedAt
) {

    public static InboundOrderResponse from(InboundOrder order) {
        return new InboundOrderResponse(
                order.getId(),
                order.getOrderNo(),
                order.getStatus(),
                order.getWarehouse().getId(),
                order.getWarehouse().getCode(),
                order.getSupplierName(),
                order.getItems().stream().map(OrderItemResponse::from).toList(),
                order.getReceivedAt(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
