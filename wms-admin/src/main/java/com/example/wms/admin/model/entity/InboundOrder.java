package com.example.wms.admin.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.wms.common.enums.InboundOrderStatus;
import com.example.wms.common.enums.SourceType;
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
    private Long supplierId;
    private Instant receivedAt;
    private SourceType sourceType = SourceType.MANUAL;
    private String sourceOrderNo;
    private String externalOrderNo;
    @TableLogic(value = "1", delval = "0")
    private boolean enabled = true;
    private Instant createdAt;
    private Instant updatedAt;

    @TableField(exist = false)
    private Warehouse warehouse;

    @TableField(exist = false)
    private Supplier supplier;

    @TableField(exist = false)
    private List<InboundOrderItem> items = new ArrayList<>();

    public InboundOrder(String orderNo, Warehouse warehouse, Supplier supplier) {
        this.orderNo = orderNo;
        this.warehouse = warehouse;
        this.warehouseId = warehouse.getId();
        if (supplier != null) {
            this.supplier = supplier;
            this.supplierId = supplier.getId();
        }
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void addItem(Sku sku, WarehouseArea area, WarehouseLocation location, int quantity) {
        this.items.add(new InboundOrderItem(this, sku, area, location, quantity));
    }

    public void attachWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
        this.warehouseId = warehouse.getId();
    }

    public void attachSupplier(Supplier supplier) {
        this.supplier = supplier;
    }

    public void updateSupplier(Supplier supplier) {
        this.supplier = supplier;
        this.supplierId = supplier != null ? supplier.getId() : null;
        this.updatedAt = Instant.now();
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
