package com.example.wms.admin.view.dto;

import com.example.wms.admin.model.entity.InboundOrderItem;
import com.example.wms.admin.model.entity.OutboundOrderItem;

public record OrderItemResponse(
        Long skuId,
        String skuCode,
        String skuName,
        int quantity,
        Long areaId,
        String areaCode,
        Long locationId,
        String locationCode
) {

    public static OrderItemResponse from(InboundOrderItem item) {
        return new OrderItemResponse(
                item.getSku().getId(),
                item.getSku().getCode(),
                item.getSku().getName(),
                item.getQuantity(),
                item.getArea().getId(),
                item.getArea().getAreaCode(),
                item.getLocation().getId(),
                item.getLocation().getLocationCode()
        );
    }

    public static OrderItemResponse from(OutboundOrderItem item) {
        return new OrderItemResponse(
                item.getSku().getId(),
                item.getSku().getCode(),
                item.getSku().getName(),
                item.getQuantity(),
                null,
                null,
                null,
                null
        );
    }
}
