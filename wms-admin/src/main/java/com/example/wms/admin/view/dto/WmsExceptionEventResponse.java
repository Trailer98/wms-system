package com.example.wms.admin.view.dto;

import com.example.wms.admin.model.entity.WmsExceptionEvent;
import com.example.wms.common.enums.ExceptionType;

import java.time.Instant;

public record WmsExceptionEventResponse(
        Long id,
        ExceptionType exceptionType,
        String bizNo,
        Long skuId,
        String skuCode,
        String skuName,
        Long warehouseId,
        String warehouseCode,
        String warehouseName,
        Long areaId,
        String areaCode,
        Long locationId,
        String locationCode,
        String message,
        String status,
        Long handlerId,
        Instant handledTime,
        Instant createTime
) {

    public static WmsExceptionEventResponse from(WmsExceptionEvent event) {
        return new WmsExceptionEventResponse(
                event.getId(),
                event.getExceptionType(),
                event.getBizNo(),
                event.getSkuId(),
                event.getSku() != null ? event.getSku().getCode() : null,
                event.getSku() != null ? event.getSku().getName() : null,
                event.getWarehouseId(),
                event.getWarehouse() != null ? event.getWarehouse().getCode() : null,
                event.getWarehouse() != null ? event.getWarehouse().getName() : null,
                event.getAreaId(),
                event.getArea() != null ? event.getArea().getAreaCode() : null,
                event.getLocationId(),
                event.getLocation() != null ? event.getLocation().getLocationCode() : null,
                event.getMessage(),
                event.getStatus(),
                event.getHandlerId(),
                event.getHandledTime(),
                event.getCreateTime()
        );
    }
}
