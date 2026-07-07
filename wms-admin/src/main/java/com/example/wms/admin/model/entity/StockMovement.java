package com.example.wms.admin.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.wms.common.enums.MovementType;
import com.example.wms.common.enums.OperationType;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@TableName("stock_movements")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockMovement {

    private Long id;
    private MovementType type;
    private OperationType operationType;
    private Long warehouseId;
    private Long skuId;
    private Long areaId;
    private Long locationId;
    private int quantityChange;
    private Integer beforeQuantity;
    private Integer afterQuantity;
    private Integer beforeReservedQuantity;
    private Integer afterReservedQuantity;
    private Integer beforeFrozenQuantity;
    private Integer afterFrozenQuantity;
    private Integer beforeAvailableQuantity;
    private Integer afterAvailableQuantity;
    private String businessNo;
    private String remark;
    private String operator = "admin";
    private Instant occurredAt;
    @TableLogic(value = "1", delval = "0")
    private boolean enabled = true;

    @TableField(exist = false)
    private Warehouse warehouse;

    @TableField(exist = false)
    private Sku sku;

    @TableField(exist = false)
    private WarehouseArea area;

    @TableField(exist = false)
    private WarehouseLocation location;

    /**
     * before/after must be captured at the exact moment of the inventory change (never re-derived
     * from the current live row afterwards), since this table is an audit trail of historical state.
     */
    public StockMovement(
            MovementType type,
            OperationType operationType,
            Warehouse warehouse,
            WarehouseArea area,
            WarehouseLocation location,
            Sku sku,
            int quantityChange,
            InventorySnapshot before,
            InventorySnapshot after,
            String businessNo,
            String remark
    ) {
        this.type = type;
        this.operationType = operationType;
        this.warehouse = warehouse;
        this.warehouseId = warehouse.getId();
        this.area = area;
        this.areaId = area != null ? area.getId() : null;
        this.location = location;
        this.locationId = location != null ? location.getId() : null;
        this.sku = sku;
        this.skuId = sku.getId();
        this.quantityChange = quantityChange;
        this.beforeQuantity = before.onHandQty();
        this.afterQuantity = after.onHandQty();
        this.beforeReservedQuantity = before.lockedQty();
        this.afterReservedQuantity = after.lockedQty();
        this.beforeFrozenQuantity = before.frozenQty();
        this.afterFrozenQuantity = after.frozenQty();
        this.beforeAvailableQuantity = before.availableQty();
        this.afterAvailableQuantity = after.availableQty();
        this.businessNo = businessNo;
        this.remark = remark;
        this.occurredAt = Instant.now();
    }

    public void attachWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
    }

    public void attachSku(Sku sku) {
        this.sku = sku;
    }

    public void attachArea(WarehouseArea area) {
        this.area = area;
    }

    public void attachLocation(WarehouseLocation location) {
        this.location = location;
    }
}
