package com.example.wms.admin.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.wms.common.enums.StockCountItemStatus;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@TableName("stock_count_item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockCountItem {

    private Long id;
    private Long countTaskId;
    private Long skuId;
    private Long warehouseId;
    private Long areaId;
    private Long locationId;
    private int bookOnHandQty;
    private int bookLockedQty;
    private int bookFrozenQty;
    private int bookAvailableQty;
    private Integer actualQty;
    private Integer diffQty;
    private StockCountItemStatus status = StockCountItemStatus.PENDING;
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

    public StockCountItem(StockCountTask task, Sku sku, WarehouseArea area, WarehouseLocation location, InventorySnapshot bookSnapshot) {
        this.countTaskId = task.getId();
        this.sku = sku;
        this.skuId = sku.getId();
        this.warehouseId = task.getWarehouseId();
        this.area = area;
        this.areaId = area.getId();
        this.location = location;
        this.locationId = location.getId();
        this.bookOnHandQty = bookSnapshot.onHandQty();
        this.bookLockedQty = bookSnapshot.lockedQty();
        this.bookFrozenQty = bookSnapshot.frozenQty();
        this.bookAvailableQty = bookSnapshot.availableQty();
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
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

    public void recordActual(int actualQty, String remark) {
        this.actualQty = actualQty;
        this.diffQty = actualQty - this.bookOnHandQty;
        this.status = StockCountItemStatus.RECORDED;
        if (remark != null) {
            this.remark = remark;
        }
        this.updatedAt = Instant.now();
    }
}
