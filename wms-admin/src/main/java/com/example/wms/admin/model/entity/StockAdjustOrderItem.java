package com.example.wms.admin.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.wms.common.enums.AdjustAction;
import com.example.wms.common.enums.AdjustType;
import com.example.wms.common.enums.HoldStatus;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@TableName("stock_adjust_order_item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockAdjustOrderItem {

    private Long id;
    private Long adjustOrderId;
    private Long inventoryId;
    private Long skuId;
    private Long warehouseId;
    private Long areaId;
    private Long locationId;
    private AdjustType adjustType;
    private AdjustAction adjustAction;
    private int adjustQty;
    private Long targetWarehouseId;
    private Long targetAreaId;
    private Long targetLocationId;
    private Long targetInventoryId;
    private int holdQty;
    private HoldStatus holdStatus = HoldStatus.NONE;
    private Integer beforeOnHandQty;
    private Integer afterOnHandQty;
    private Integer beforeLockedQty;
    private Integer afterLockedQty;
    private Integer beforeFrozenQty;
    private Integer afterFrozenQty;
    private Integer beforeAvailableQty;
    private Integer afterAvailableQty;
    private String remark;
    @TableLogic(value = "1", delval = "0")
    private boolean enabled = true;
    private Instant createdAt;
    private Instant updatedAt;

    @TableField(exist = false)
    private Sku sku;

    @TableField(exist = false)
    private WarehouseArea area;

    @TableField(exist = false)
    private WarehouseLocation location;

    @TableField(exist = false)
    private WarehouseArea targetArea;

    @TableField(exist = false)
    private WarehouseLocation targetLocation;

    /** Plain quantity adjustment (increase/decrease); no transfer target. */
    public StockAdjustOrderItem(StockAdjustOrder order, Long inventoryId, Sku sku, WarehouseArea area, WarehouseLocation location,
            AdjustType adjustType, AdjustAction adjustAction, int adjustQty, String remark) {
        this(order, inventoryId, sku, area, location, adjustType, adjustAction, adjustQty, remark, null, null, null);
    }

    /** Transfer between normal and exception areas; target* is the destination side, resolved at create time. */
    public StockAdjustOrderItem(StockAdjustOrder order, Long inventoryId, Sku sku, WarehouseArea area, WarehouseLocation location,
            AdjustType adjustType, AdjustAction adjustAction, int adjustQty, String remark,
            Long targetWarehouseId, WarehouseArea targetArea, WarehouseLocation targetLocation) {
        this.adjustOrderId = order.getId();
        this.inventoryId = inventoryId;
        this.sku = sku;
        this.skuId = sku.getId();
        this.warehouseId = order.getWarehouseId();
        this.area = area;
        this.areaId = area.getId();
        this.location = location;
        this.locationId = location.getId();
        this.adjustType = adjustType;
        this.adjustAction = adjustAction;
        this.adjustQty = adjustQty;
        this.remark = remark;
        this.targetWarehouseId = targetWarehouseId;
        this.targetArea = targetArea;
        this.targetAreaId = targetArea != null ? targetArea.getId() : null;
        this.targetLocation = targetLocation;
        this.targetLocationId = targetLocation != null ? targetLocation.getId() : null;
        this.holdQty = 0;
        this.holdStatus = HoldStatus.NONE;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void assignOrderId(Long adjustOrderId) {
        this.adjustOrderId = adjustOrderId;
    }

    public void assignInventoryId(Long inventoryId) {
        this.inventoryId = inventoryId;
    }

    public void assignTargetInventoryId(Long targetInventoryId) {
        this.targetInventoryId = targetInventoryId;
    }

    public void attachSku(Sku sku) {
        this.sku = sku;
        this.skuId = sku.getId();
    }

    public void attachArea(WarehouseArea area) {
        this.area = area;
        this.areaId = area.getId();
    }

    public void attachLocation(WarehouseLocation location) {
        this.location = location;
        this.locationId = location.getId();
    }

    public void attachTargetArea(WarehouseArea targetArea) {
        this.targetArea = targetArea;
        this.targetAreaId = targetArea.getId();
    }

    public void attachTargetLocation(WarehouseLocation targetLocation) {
        this.targetLocation = targetLocation;
        this.targetLocationId = targetLocation.getId();
    }

    /** Submit-time freeze for a pending transfer-to-exception. */
    public void markHeld(int qty) {
        this.holdQty = qty;
        this.holdStatus = HoldStatus.HELD;
        this.updatedAt = Instant.now();
    }

    /** Cancel of a submitted-but-not-confirmed transfer releases its hold. */
    public void markReleased() {
        this.holdStatus = HoldStatus.RELEASED;
        this.updatedAt = Instant.now();
    }

    /** Confirm consumes the hold (or, for restore, simply marks the transfer done). */
    public void markConsumed() {
        this.holdStatus = HoldStatus.CONSUMED;
        this.updatedAt = Instant.now();
    }

    public void recordMovement(StockMovement movement) {
        this.beforeOnHandQty = movement.getBeforeQuantity();
        this.afterOnHandQty = movement.getAfterQuantity();
        this.beforeLockedQty = movement.getBeforeReservedQuantity();
        this.afterLockedQty = movement.getAfterReservedQuantity();
        this.beforeFrozenQty = movement.getBeforeFrozenQuantity();
        this.afterFrozenQty = movement.getAfterFrozenQuantity();
        this.beforeAvailableQty = movement.getBeforeAvailableQuantity();
        this.afterAvailableQty = movement.getAfterAvailableQuantity();
        this.updatedAt = Instant.now();
    }
}
