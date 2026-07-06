package com.example.wms.admin.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.wms.common.enums.OutboundOrderStatus;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@TableName("outbound_orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboundOrder {

    private Long id;
    private String orderNo;
    private OutboundOrderStatus status = OutboundOrderStatus.CREATED;
    private Long warehouseId;
    private Long customerId;
    private Instant shippedAt;
    @TableLogic(value = "1", delval = "0")
    private boolean enabled = true;
    private Instant createdAt;
    private Instant updatedAt;

    @TableField(exist = false)
    private Warehouse warehouse;

    @TableField(exist = false)
    private Customer customer;

    @TableField(exist = false)
    private List<OutboundOrderItem> items = new ArrayList<>();

    @TableField(exist = false)
    private List<OutboundStockLock> allocations = new ArrayList<>();

    public OutboundOrder(String orderNo, Warehouse warehouse, Customer customer) {
        this.orderNo = orderNo;
        this.warehouse = warehouse;
        this.warehouseId = warehouse.getId();
        if (customer != null) {
            this.customer = customer;
            this.customerId = customer.getId();
        }
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void addItem(Sku sku, int quantity) {
        this.items.add(new OutboundOrderItem(this, sku, quantity));
    }

    public void attachWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
        this.warehouseId = warehouse.getId();
    }

    public void attachCustomer(Customer customer) {
        this.customer = customer;
    }

    public void updateCustomer(Customer customer) {
        this.customer = customer;
        this.customerId = customer != null ? customer.getId() : null;
        this.updatedAt = Instant.now();
    }

    public void setItems(List<OutboundOrderItem> items) {
        this.items = items;
    }

    public void setAllocations(List<OutboundStockLock> allocations) {
        this.allocations = allocations;
    }

    public void markLocked() {
        this.status = OutboundOrderStatus.LOCKED;
        this.updatedAt = Instant.now();
    }

    public void markShipped() {
        this.status = OutboundOrderStatus.SHIPPED;
        this.shippedAt = Instant.now();
        this.updatedAt = this.shippedAt;
    }

    public void markCancelled() {
        this.status = OutboundOrderStatus.CANCELLED;
        this.updatedAt = Instant.now();
    }
}
