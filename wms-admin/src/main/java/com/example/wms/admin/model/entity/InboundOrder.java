package com.example.wms.admin.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.wms.common.enums.InboundOrderStatus;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@TableName("inbound_orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InboundOrder {
    private Long id;
    private String orderNo;
    private InboundOrderStatus status = InboundOrderStatus.CREATED;
    private Long warehouseId;
    private String supplierName;
    private Instant receivedAt;
    @TableLogic(value = "1", delval = "0")
    private boolean enabled = true;
    private Instant createdAt;
    private Instant updatedAt;

    @TableField(exist = false)
    private Warehouse warehouse;

    @TableField(exist = false)
    private List<InboundOrderItem> items = new ArrayList<>();

    public InboundOrder(String orderNo, Warehouse warehouse, String supplierName) {
        this.orderNo = orderNo;
        this.warehouse = warehouse;
        this.warehouseId = warehouse.getId();
        this.supplierName = supplierName;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void addItem(Sku sku, int quantity) {
        this.items.add(new InboundOrderItem(this, sku, quantity));
    }

    public void attachWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
        this.warehouseId = warehouse.getId();
    }

    public void setItems(List<InboundOrderItem> items) {
        this.items = items;
    }

    public void markReceived() {
        this.status = InboundOrderStatus.RECEIVED;
        this.receivedAt = Instant.now();
        this.updatedAt = this.receivedAt;
    }
}
