package com.example.wms.admin.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.wms.common.enums.AreaType;
import com.example.wms.common.enums.InventoryStatus;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@TableName("inventory")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Inventory {
    private Long id;
    private Long warehouseId;
    private Long skuId;
    private Long areaId;
    private Long locationId;
    private InventoryStatus inventoryStatus = InventoryStatus.NORMAL;
    private int quantity;
    private int reservedQuantity;
    private int frozenQuantity;
    @TableLogic(value = "1", delval = "0")
    private boolean enabled = true;
    private Instant createdAt;
    private Instant updatedAt;

    @TableField(exist = false)
    private Warehouse warehouse;

    @TableField(exist = false)
    private Sku sku;

    @TableField(exist = false)
    private WarehouseArea area;

    @TableField(exist = false)
    private WarehouseLocation location;

    public Inventory(Warehouse warehouse, WarehouseArea area, WarehouseLocation location, Sku sku) {
        this.warehouse = warehouse;
        this.warehouseId = warehouse.getId();
        this.area = area;
        this.areaId = area.getId();
        this.location = location;
        this.locationId = location.getId();
        this.sku = sku;
        this.skuId = sku.getId();
        this.inventoryStatus = area.getAreaType() == AreaType.EXCEPTION ? InventoryStatus.EXCEPTION : InventoryStatus.NORMAL;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void attachWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
        this.warehouseId = warehouse.getId();
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

    public void increase(int amount) {
        this.quantity += amount;
        this.updatedAt = Instant.now();
    }

    public void freeze(int amount) {
        this.frozenQuantity += amount;
        this.updatedAt = Instant.now();
    }

    public int getAvailableQuantity() {
        return quantity - reservedQuantity - frozenQuantity;
    }

    public InventorySnapshot snapshot() {
        return new InventorySnapshot(quantity, reservedQuantity, frozenQuantity);
    }
}
