package com.example.wms.admin.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.wms.common.enums.LocationStatus;
import com.example.wms.common.enums.LocationType;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@TableName("warehouse_locations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WarehouseLocation {

    private Long id;
    private Long warehouseId;
    private Long areaId;
    private String locationCode;
    private String locationName;
    private LocationType locationType;
    private LocationStatus status = LocationStatus.ENABLED;
    private int capacityQty;
    private int usedQty;
    private boolean allowMixedSku = true;
    private int pickPriority;
    private String remark;
    @TableLogic(value = "1", delval = "0")
    private boolean enabled = true;
    private Instant createdAt;
    private Instant updatedAt;

    @TableField(exist = false)
    private Warehouse warehouse;

    @TableField(exist = false)
    private WarehouseArea area;

    public WarehouseLocation(
            Warehouse warehouse,
            WarehouseArea area,
            String locationCode,
            String locationName,
            LocationType locationType,
            int capacityQty,
            boolean allowMixedSku,
            int pickPriority,
            String remark
    ) {
        this.warehouse = warehouse;
        this.warehouseId = warehouse.getId();
        this.area = area;
        this.areaId = area.getId();
        this.locationCode = locationCode;
        this.locationName = locationName;
        this.locationType = locationType;
        this.capacityQty = capacityQty;
        this.allowMixedSku = allowMixedSku;
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

    public void attachArea(WarehouseArea area) {
        this.area = area;
        this.areaId = area.getId();
    }

    public void update(String locationName, LocationType locationType, int capacityQty, boolean allowMixedSku, int pickPriority, String remark) {
        if (capacityQty < usedQty) {
            throw new IllegalArgumentException("capacity qty must not be less than used qty");
        }
        this.locationName = locationName;
        this.locationType = locationType;
        this.capacityQty = capacityQty;
        this.allowMixedSku = allowMixedSku;
        this.pickPriority = pickPriority;
        this.remark = remark;
        this.updatedAt = Instant.now();
    }

    public void changeStatus(LocationStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public boolean isUsable() {
        return status == LocationStatus.ENABLED;
    }

    public int getAvailableCapacity() {
        return capacityQty - usedQty;
    }
}
