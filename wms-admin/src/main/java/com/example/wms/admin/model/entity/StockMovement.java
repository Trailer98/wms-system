package com.example.wms.admin.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.wms.common.enums.MovementType;
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
    private Long warehouseId;
    private Long skuId;
    private int quantityChange;
    private String businessNo;
    private String remark;
    private Instant occurredAt;

    @TableField(exist = false)
    private Warehouse warehouse;

    @TableField(exist = false)
    private Sku sku;

    public StockMovement(MovementType type, Warehouse warehouse, Sku sku, int quantityChange, String businessNo, String remark) {
        this.type = type;
        this.warehouse = warehouse;
        this.warehouseId = warehouse.getId();
        this.sku = sku;
        this.skuId = sku.getId();
        this.quantityChange = quantityChange;
        this.businessNo = businessNo;
        this.remark = remark;
        this.occurredAt = Instant.now();
    }
}
