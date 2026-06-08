package com.example.wms.admin.view.dto;

import com.example.wms.admin.model.entity.InboundOrderItem;
import com.example.wms.admin.model.entity.OutboundOrderItem;

public record OrderItemResponse(
        Long skuId,
        String skuCode,
        String skuName,
        int quantity
) {

    public static OrderItemResponse from(InboundOrderItem item) {
        return new OrderItemResponse(
                item.getSku().getId(),
                item.getSku().getCode(),
                item.getSku().getName(),
                item.getQuantity()
        );
    }

    public static OrderItemResponse from(OutboundOrderItem item) {
        return new OrderItemResponse(
                item.getSku().getId(),
                item.getSku().getCode(),
                item.getSku().getName(),
                item.getQuantity()
        );
    }
}
