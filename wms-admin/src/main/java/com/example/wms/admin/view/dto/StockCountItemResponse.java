package com.example.wms.admin.view.dto;

import com.example.wms.admin.model.entity.StockCountItem;
import com.example.wms.common.enums.StockCountItemStatus;

public record StockCountItemResponse(
        Long id,
        Long skuId,
        String skuCode,
        String skuName,
        Long areaId,
        String areaCode,
        Long locationId,
        String locationCode,
        int bookOnHandQty,
        int bookLockedQty,
        int bookFrozenQty,
        int bookAvailableQty,
        Integer actualQty,
        Integer diffQty,
        StockCountItemStatus status,
        String remark
) {

    public static StockCountItemResponse from(StockCountItem item) {
        return new StockCountItemResponse(
                item.getId(),
                item.getSku().getId(),
                item.getSku().getCode(),
                item.getSku().getName(),
                item.getArea().getId(),
                item.getArea().getAreaCode(),
                item.getLocation().getId(),
                item.getLocation().getLocationCode(),
                item.getBookOnHandQty(),
                item.getBookLockedQty(),
                item.getBookFrozenQty(),
                item.getBookAvailableQty(),
                item.getActualQty(),
                item.getDiffQty(),
                item.getStatus(),
                item.getRemark()
        );
    }
}
