package com.example.wms.admin.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.wms.common.enums.AreaStatus;
import com.example.wms.common.enums.AreaType;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@TableName("warehouse_areas")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WarehouseArea {

    private Long id;
    private Long warehouseId;
    private String areaCode;
    private String areaName;
    private AreaType areaType;
    private AreaStatus status = AreaStatus.ENABLED;
    private int pickPriority;
    private String remark;
    @TableLogic(value = "1", delval = "0")
    private boolean enabled = true;
    private Instant createdAt;
    private Instant updatedAt;

    @TableField(exist = false)
    private Warehouse warehouse;

    public WarehouseArea(Warehouse warehouse, String areaCode, String areaName, AreaType areaType, int pickPriority, String remark) {
        this.warehouse = warehouse;
        this.warehouseId = warehouse.getId();
        this.areaCode = areaCode;
        this.areaName = areaName;
        this.areaType = areaType;
        this.pickPriority = pickPriority;
        this.remark = remark;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void attachWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
        this.warehouseId = warehouse.getId();
    }

    public void update(String areaName, AreaType areaType, int pickPriority, String remark) {
        this.areaName = areaName;
        this.areaType = areaType;
        this.pickPriority = pickPriority;
        this.remark = remark;
        this.updatedAt = Instant.now();
    }

    public void changeStatus(AreaStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public boolean isUsable() {
        return status == AreaStatus.ENABLED;
    }
}
