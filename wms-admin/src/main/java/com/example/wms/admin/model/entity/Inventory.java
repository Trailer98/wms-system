package com.example.wms.admin.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
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
    private int quantity;
    private int reservedQuantity;
    @TableLogic(value = "1", delval = "0")
    private boolean enabled = true;
    private Instant createdAt;
    private Instant updatedAt;

    @TableField(exist = false)
    private Warehouse warehouse;

    @TableField(exist = false)
    private Sku sku;

    public Inventory(Warehouse warehouse, Sku sku) {
        this.warehouse = warehouse;
        this.warehouseId = warehouse.getId();
        this.sku = sku;
        this.skuId = sku.getId();
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

    public void increase(int amount) {
        this.quantity += amount;
        this.updatedAt = Instant.now();
    }

    public void decrease(int amount) {
        if (getAvailableQuantity() < amount) {
            throw new IllegalArgumentException("available inventory is insufficient");
        }
        this.quantity -= amount;
        this.updatedAt = Instant.now();
    }

    public int getAvailableQuantity() {
        return quantity - reservedQuantity;
    }
}
