package com.example.wms.admin.view.dto;

import com.example.wms.admin.model.entity.OutboundStockLock;
import com.example.wms.common.enums.LockStatus;

public record AllocationResponse(
        Long id,
        Long skuId,
        String skuCode,
        String skuName,
        Long warehouseId,
        String warehouseCode,
        Long areaId,
        String areaCode,
        Long locationId,
        String locationCode,
        int lockQty,
        int shippedQty,
        LockStatus status
) {

    public static AllocationResponse from(OutboundStockLock lock) {
        return new AllocationResponse(
                lock.getId(),
                lock.getSku().getId(),
                lock.getSku().getCode(),
                lock.getSku().getName(),
                lock.getWarehouse().getId(),
                lock.getWarehouse().getCode(),
                lock.getArea().getId(),
                lock.getArea().getAreaCode(),
                lock.getLocation().getId(),
                lock.getLocation().getLocationCode(),
                lock.getLockQty(),
                lock.getShippedQty(),
                lock.getStatus()
        );
    }
}
