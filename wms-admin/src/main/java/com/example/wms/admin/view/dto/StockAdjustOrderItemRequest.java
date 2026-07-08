package com.example.wms.admin.view.dto;

import com.example.wms.common.enums.AdjustAction;
import com.example.wms.common.enums.AdjustType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * When {@code inventoryId} is set, it is the sole source of truth for which sku/warehouse/area/
 * location gets adjusted — {@code skuId}/{@code areaId}/{@code locationId} are ignored server-side
 * in that case (kept only so older clients that still send them don't break bean validation).
 * When {@code inventoryId} is null (an "off-book" increase), those three fields become required and
 * {@code allowCreateInventory} must be true; only {@code adjustType == INCREASE} is allowed.
 * <p>
 * {@code adjustAction} is the real source of truth for what this item does (see {@link AdjustAction});
 * {@code adjustType} is kept only for backward compatibility and, for a transfer/restore action, is
 * ignored server-side entirely (it's derived from {@code adjustAction} instead — see
 * {@code StockAdjustOrderService}). If a client omits {@code adjustAction} altogether, the server
 * infers it from {@code adjustType} (INCREASE→QUANTITY_INCREASE, DECREASE→QUANTITY_DECREASE).
 * {@code targetWarehouseId}/{@code targetAreaId}/{@code targetLocationId} are only meaningful for
 * {@code TRANSFER_TO_EXCEPTION}/{@code RESTORE_FROM_EXCEPTION} and identify the destination balance;
 * {@code targetWarehouseId} must equal the order's warehouse if given (cross-warehouse transfer is
 * not supported) and defaults to it if omitted.
 */
public record StockAdjustOrderItemRequest(
        Long inventoryId,
        Long skuId,
        Long areaId,
        Long locationId,
        @NotNull AdjustType adjustType,
        AdjustAction adjustAction,
        @Positive int adjustQty,
        boolean allowCreateInventory,
        Long targetWarehouseId,
        Long targetAreaId,
        Long targetLocationId,
        String remark
) {

    /** Legacy 8-arg shape (no adjustAction/target fields) kept so pre-existing callers still compile. */
    public StockAdjustOrderItemRequest(Long inventoryId, Long skuId, Long areaId, Long locationId,
            AdjustType adjustType, int adjustQty, boolean allowCreateInventory, String remark) {
        this(inventoryId, skuId, areaId, locationId, adjustType, null, adjustQty, allowCreateInventory, null, null, null, remark);
    }
}
