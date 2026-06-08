package com.example.wms.admin.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@TableName("outbound_order_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboundOrderItem {

    private Long id;
    private Long orderId;
    private Long skuId;
    private int quantity;

    @TableField(exist = false)
    private Sku sku;

    public OutboundOrderItem(OutboundOrder order, Sku sku, int quantity) {
        this.orderId = order.getId();
        this.sku = sku;
        this.skuId = sku.getId();
        this.quantity = quantity;
    }

    public void assignOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public void attachSku(Sku sku) {
        this.sku = sku;
        this.skuId = sku.getId();
    }
}
