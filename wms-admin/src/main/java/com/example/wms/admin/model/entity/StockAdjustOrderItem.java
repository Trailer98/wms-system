package com.example.wms.admin.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.wms.common.enums.AdjustType;
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
    private int adjustQty;
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

    public StockAdjustOrderItem(StockAdjustOrder order, Long inventoryId, Sku sku, WarehouseArea area, WarehouseLocation location, AdjustType adjustType, int adjustQty, String remark) {
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
        this.adjustQty = adjustQty;
        this.remark = remark;
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
