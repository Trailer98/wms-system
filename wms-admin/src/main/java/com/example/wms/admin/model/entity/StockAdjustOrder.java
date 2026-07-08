package com.example.wms.admin.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.wms.common.enums.AdjustAction;
import com.example.wms.common.enums.AdjustReasonType;
import com.example.wms.common.enums.AdjustType;
import com.example.wms.common.enums.StockAdjustOrderStatus;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@TableName("stock_adjust_order")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockAdjustOrder {

    private Long id;
    private String adjustNo;
    private StockAdjustOrderStatus status = StockAdjustOrderStatus.DRAFT;
    private AdjustReasonType reasonType;
    private String reason;
    private Long warehouseId;
    private String createdBy;
    private String confirmedBy;
    private Instant confirmedAt;
    private String cancelledBy;
    private Instant cancelledAt;
    private String cancelReason;
    @TableLogic(value = "1", delval = "0")
    private boolean enabled = true;
    private Instant createdAt;
    private Instant updatedAt;

    @TableField(exist = false)
    private Warehouse warehouse;

    @TableField(exist = false)
    private List<StockAdjustOrderItem> items = new ArrayList<>();

    public StockAdjustOrder(String adjustNo, Warehouse warehouse, AdjustReasonType reasonType, String reason, String createdBy) {
        this.adjustNo = adjustNo;
        this.warehouse = warehouse;
        this.warehouseId = warehouse.getId();
        this.reasonType = reasonType;
        this.reason = reason;
        this.createdBy = createdBy;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(AdjustReasonType reasonType, String reason) {
        this.reasonType = reasonType;
        this.reason = reason;
        this.updatedAt = Instant.now();
    }

    public void addItem(Long inventoryId, Sku sku, WarehouseArea area, WarehouseLocation location,
            AdjustType adjustType, AdjustAction adjustAction, int adjustQty, String remark) {
        this.items.add(new StockAdjustOrderItem(this, inventoryId, sku, area, location, adjustType, adjustAction, adjustQty, remark));
    }

    /** Transfer/restore item: also anchors the destination area/location resolved at create time. */
    public void addTransferItem(Long inventoryId, Sku sku, WarehouseArea area, WarehouseLocation location,
            AdjustAction adjustAction, int adjustQty, String remark,
            Long targetWarehouseId, WarehouseArea targetArea, WarehouseLocation targetLocation) {
        this.items.add(new StockAdjustOrderItem(this, inventoryId, sku, area, location,
                AdjustType.DECREASE, adjustAction, adjustQty, remark, targetWarehouseId, targetArea, targetLocation));
    }

    public void attachWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
        this.warehouseId = warehouse.getId();
    }

    public void setItems(List<StockAdjustOrderItem> items) {
        this.items = items;
    }

    public void markSubmitted() {
        this.status = StockAdjustOrderStatus.SUBMITTED;
        this.updatedAt = Instant.now();
    }

    public void markCompleted(String confirmedBy) {
        this.status = StockAdjustOrderStatus.COMPLETED;
        this.confirmedBy = confirmedBy;
        this.confirmedAt = Instant.now();
        this.updatedAt = this.confirmedAt;
    }

    public void markCancelled(String cancelledBy, String cancelReason) {
        this.status = StockAdjustOrderStatus.CANCELLED;
        this.cancelledBy = cancelledBy;
        this.cancelReason = cancelReason;
        this.cancelledAt = Instant.now();
        this.updatedAt = this.cancelledAt;
    }
}
