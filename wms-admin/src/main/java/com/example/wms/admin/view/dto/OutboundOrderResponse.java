package com.example.wms.admin.view.dto;

import com.example.wms.admin.model.entity.OutboundOrder;
import com.example.wms.common.enums.OutboundOrderStatus;

import java.time.Instant;
import java.util.List;

public record OutboundOrderResponse(
        Long id,
        String orderNo,
        OutboundOrderStatus status,
        Long warehouseId,
        String warehouseCode,
        Long customerId,
        String customerCode,
        String customerName,
        List<OrderItemResponse> items,
        Instant shippedAt,
        Instant createdAt,
        Instant updatedAt
) {

    public static OutboundOrderResponse from(OutboundOrder order) {
        return new OutboundOrderResponse(
                order.getId(),
                order.getOrderNo(),
                order.getStatus(),
                order.getWarehouse().getId(),
                order.getWarehouse().getCode(),
                order.getCustomer() != null ? order.getCustomer().getId() : null,
                order.getCustomer() != null ? order.getCustomer().getCode() : null,
                order.getCustomer() != null ? order.getCustomer().getName() : null,
                order.getItems().stream().map(OrderItemResponse::from).toList(),
                order.getShippedAt(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
