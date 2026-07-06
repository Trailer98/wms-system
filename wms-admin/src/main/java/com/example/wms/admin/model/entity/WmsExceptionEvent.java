package com.example.wms.admin.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.wms.common.enums.ExceptionType;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@TableName("wms_exception_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WmsExceptionEvent {

    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_HANDLED = "HANDLED";

    private Long id;
    private ExceptionType exceptionType;
    private String bizNo;
    private Long skuId;
    private Long warehouseId;
    private Long areaId;
    private Long locationId;
    private String message;
    private String status = STATUS_OPEN;
    private Long handlerId;
    private Instant handledTime;
    private Instant createTime;

    @TableField(exist = false)
    private Sku sku;

    @TableField(exist = false)
    private Warehouse warehouse;

    @TableField(exist = false)
    private WarehouseArea area;

    @TableField(exist = false)
    private WarehouseLocation location;

    public WmsExceptionEvent(ExceptionType exceptionType, String bizNo, Long skuId, Long warehouseId, Long areaId, Long locationId, String message) {
        this.exceptionType = exceptionType;
        this.bizNo = bizNo;
        this.skuId = skuId;
        this.warehouseId = warehouseId;
        this.areaId = areaId;
        this.locationId = locationId;
        this.message = message;
        this.createTime = Instant.now();
    }

    public void attachSku(Sku sku) {
        this.sku = sku;
    }

    public void attachWarehouse(Warehouse warehouse) {
        this.warehouse = warehouse;
    }

    public void attachArea(WarehouseArea area) {
        this.area = area;
    }

    public void attachLocation(WarehouseLocation location) {
        this.location = location;
    }

    public void markHandled() {
        this.status = STATUS_HANDLED;
        this.handledTime = Instant.now();
    }
}
