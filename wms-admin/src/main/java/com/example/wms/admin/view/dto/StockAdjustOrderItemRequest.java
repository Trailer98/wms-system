package com.example.wms.admin.view.dto;

import com.example.wms.common.enums.AdjustType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * When {@code inventoryId} is set, it is the sole source of truth for which sku/warehouse/area/
 * location gets adjusted — {@code skuId}/{@code areaId}/{@code locationId} are ignored server-side
 * in that case (kept only so older clients that still send them don't break bean validation).
 * When {@code inventoryId} is null (an "off-book" increase), those three fields become required and
 * {@code allowCreateInventory} must be true; only {@code adjustType == INCREASE} is allowed.
 */
public record StockAdjustOrderItemRequest(
        Long inventoryId,
        Long skuId,
        Long areaId,
        Long locationId,
        @NotNull AdjustType adjustType,
        @Positive int adjustQty,
        boolean allowCreateInventory,
        String remark
) {
}
