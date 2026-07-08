package com.example.wms.admin.view.dto;

import com.example.wms.admin.model.entity.StockAdjustOrderItem;
import com.example.wms.common.enums.AdjustType;

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
        int adjustQty,
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
                item.getAdjustQty(),
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
