package com.example.wms.admin.view.dto;

import com.example.wms.admin.model.entity.InboundOrder;
import com.example.wms.common.enums.InboundOrderStatus;
import com.example.wms.common.enums.SourceType;

import java.time.Instant;
import java.util.List;

public record InboundOrderResponse(
        Long id,
        String orderNo,
        InboundOrderStatus status,
        Long warehouseId,
        String warehouseCode,
        Long supplierId,
        String supplierCode,
        String supplierName,
        List<OrderItemResponse> items,
        SourceType sourceType,
        String sourceOrderNo,
        String externalOrderNo,
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
                order.getSupplier() != null ? order.getSupplier().getId() : null,
                order.getSupplier() != null ? order.getSupplier().getCode() : null,
                order.getSupplier() != null ? order.getSupplier().getName() : null,
                order.getItems().stream().map(OrderItemResponse::from).toList(),
                order.getSourceType(),
                order.getSourceOrderNo(),
                order.getExternalOrderNo(),
                order.getReceivedAt(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
