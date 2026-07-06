package com.example.wms.admin.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.wms.common.enums.LockStatus;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@TableName("outbound_stock_locks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboundStockLock {

    private Long id;
    private Long outboundOrderId;
    private Long outboundOrderItemId;
    private Long skuId;
    private Long warehouseId;
    private Long areaId;
    private Long locationId;
    private int lockQty;
    private int shippedQty;
    private LockStatus status = LockStatus.LOCKED;
    private Instant createdAt;
    private Instant updatedAt;

    @TableField(exist = false)
    private Sku sku;

    @TableField(exist = false)
    private Warehouse warehouse;

    @TableField(exist = false)
    private WarehouseArea area;

    @TableField(exist = false)
    private WarehouseLocation location;

    public OutboundStockLock(Long outboundOrderId, Long outboundOrderItemId, Sku sku, Warehouse warehouse, WarehouseArea area, WarehouseLocation location, int lockQty) {
        this.outboundOrderId = outboundOrderId;
        this.outboundOrderItemId = outboundOrderItemId;
        this.sku = sku;
        this.skuId = sku.getId();
        this.warehouse = warehouse;
        this.warehouseId = warehouse.getId();
        this.area = area;
        this.areaId = area.getId();
        this.location = location;
        this.locationId = location.getId();
        this.lockQty = lockQty;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void attachSku(Sku sku) {
        this.sku = sku;
    }

    public void attachWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
    }

    public void attachArea(WarehouseArea area) {
        this.area = area;
    }

    public void attachLocation(WarehouseLocation location) {
        this.location = location;
    }

    public void markShipped() {
        this.shippedQty = this.lockQty;
        this.status = LockStatus.SHIPPED;
        this.updatedAt = Instant.now();
    }

    public void markReleased() {
        this.status = LockStatus.RELEASED;
        this.updatedAt = Instant.now();
    }
}
