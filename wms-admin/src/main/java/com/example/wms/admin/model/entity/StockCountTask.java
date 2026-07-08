package com.example.wms.admin.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.wms.common.enums.StockCountTaskStatus;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@TableName("stock_count_task")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StockCountTask {

    private Long id;
    private String countNo;
    private Long warehouseId;
    private Long areaId;
    private Long locationId;
    private StockCountTaskStatus status = StockCountTaskStatus.DRAFT;
    private String remark;
    private String createdBy;
    private String completedBy;
    private Instant completedAt;
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
    private WarehouseArea area;

    @TableField(exist = false)
    private WarehouseLocation location;

    @TableField(exist = false)
    private List<StockCountItem> items = new ArrayList<>();

    public StockCountTask(String countNo, Warehouse warehouse, WarehouseArea area, WarehouseLocation location, String remark, String createdBy) {
        this.countNo = countNo;
        this.warehouse = warehouse;
        this.warehouseId = warehouse.getId();
        if (area != null) {
            this.area = area;
            this.areaId = area.getId();
        }
        if (location != null) {
            this.location = location;
            this.locationId = location.getId();
        }
        this.remark = remark;
        this.createdBy = createdBy;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void attachWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
        this.warehouseId = warehouse.getId();
    }

    public void attachArea(WarehouseArea area) {
        this.area = area;
    }

    public void attachLocation(WarehouseLocation location) {
        this.location = location;
    }

    public void setItems(List<StockCountItem> items) {
        this.items = items;
    }

    public void markCounting() {
        this.status = StockCountTaskStatus.COUNTING;
        this.updatedAt = Instant.now();
    }

    public void markCompleted(String completedBy) {
        this.status = StockCountTaskStatus.COMPLETED;
        this.completedBy = completedBy;
        this.completedAt = Instant.now();
        this.updatedAt = this.completedAt;
    }

    public void markCancelled(String cancelledBy, String cancelReason) {
        this.status = StockCountTaskStatus.CANCELLED;
        this.cancelledBy = cancelledBy;
        this.cancelReason = cancelReason;
        this.cancelledAt = Instant.now();
        this.updatedAt = this.cancelledAt;
    }
}
