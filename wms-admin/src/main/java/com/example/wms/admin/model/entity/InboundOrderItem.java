package com.example.wms.admin.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@TableName("inbound_order_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InboundOrderItem {

    private Long id;
    private Long orderId;
    private Long skuId;
    private Long areaId;
    private Long locationId;
    private int quantity;
    @TableLogic(value = "1", delval = "0")
    private boolean enabled = true;

    @TableField(exist = false)
    private Sku sku;

    @TableField(exist = false)
    private WarehouseArea area;

    @TableField(exist = false)
    private WarehouseLocation location;

    public InboundOrderItem(InboundOrder order, Sku sku, WarehouseArea area, WarehouseLocation location, int quantity) {
        this.orderId = order.getId();
        this.sku = sku;
        this.skuId = sku.getId();
        this.area = area;
        this.areaId = area.getId();
        this.location = location;
        this.locationId = location.getId();
        this.quantity = quantity;
    }

    public void assignOrderId(Long orderId) {
        this.orderId = orderId;
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
}
