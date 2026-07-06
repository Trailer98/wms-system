package com.example.wms.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum ExceptionType {
    SKU_NOT_FOUND("SKU_NOT_FOUND", "SKU不存在"),
    LOCATION_DISABLED("LOCATION_DISABLED", "库位已停用"),
    LOCATION_LOCKED("LOCATION_LOCKED", "库位已锁定"),
    LOCATION_COUNTING("LOCATION_COUNTING", "库位盘点中"),
    LOCATION_CAPACITY_NOT_ENOUGH("LOCATION_CAPACITY_NOT_ENOUGH", "库位容量不足"),
    INVENTORY_NOT_ENOUGH("INVENTORY_NOT_ENOUGH", "库存不足"),
    MIXED_SKU_NOT_ALLOWED("MIXED_SKU_NOT_ALLOWED", "库位不允许混放SKU"),
    ORDER_STATUS_INVALID("ORDER_STATUS_INVALID", "单据状态不允许该操作"),
    DUPLICATE_OPERATION("DUPLICATE_OPERATION", "重复操作");

    @EnumValue
    private final String code;
    private final String label;

    ExceptionType(String code, String label) {
        this.code = code;
        this.label = label;
    }
}
