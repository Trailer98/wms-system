package com.example.wms.admin.view.dto;

import com.example.wms.admin.model.entity.StockAdjustOrderItem;
import com.example.wms.common.enums.AdjustAction;
import com.example.wms.common.enums.AdjustType;
import com.example.wms.common.enums.HoldStatus;

public record StockAdjustOrderItemResponse(
        Long id,
        Long inventoryId,
        boolean offBookIncrease,
        Long skuId,
        String skuCode,
        String skuName,
        Long areaId,
        String areaCode,
        Long locationId,
        String locationCode,
        AdjustType adjustType,
        AdjustAction adjustAction,
        int adjustQty,
        Long targetWarehouseId,
        Long targetAreaId,
        String targetAreaCode,
        Long targetLocationId,
        String targetLocationCode,
        Long targetInventoryId,
        int holdQty,
        HoldStatus holdStatus,
        Integer beforeOnHandQty,
        Integer afterOnHandQty,
        Integer beforeLockedQty,
        Integer afterLockedQty,
        Integer beforeFrozenQty,
        Integer afterFrozenQty,
        Integer beforeAvailableQty,
        Integer afterAvailableQty,
        String remark
) {

    public static StockAdjustOrderItemResponse from(StockAdjustOrderItem item) {
        return new StockAdjustOrderItemResponse(
                item.getId(),
                item.getInventoryId(),
                item.getInventoryId() == null,
                item.getSku().getId(),
                item.getSku().getCode(),
                item.getSku().getName(),
                item.getArea().getId(),
                item.getArea().getAreaCode(),
                item.getLocation().getId(),
                item.getLocation().getLocationCode(),
                item.getAdjustType(),
                item.getAdjustAction(),
                item.getAdjustQty(),
                item.getTargetWarehouseId(),
                item.getTargetAreaId(),
                item.getTargetArea() != null ? item.getTargetArea().getAreaCode() : null,
                item.getTargetLocationId(),
                item.getTargetLocation() != null ? item.getTargetLocation().getLocationCode() : null,
                item.getTargetInventoryId(),
                item.getHoldQty(),
                item.getHoldStatus(),
                item.getBeforeOnHandQty(),
                item.getAfterOnHandQty(),
                item.getBeforeLockedQty(),
                item.getAfterLockedQty(),
                item.getBeforeFrozenQty(),
                item.getAfterFrozenQty(),
                item.getBeforeAvailableQty(),
                item.getAfterAvailableQty(),
                item.getRemark()
        );
    }
}
