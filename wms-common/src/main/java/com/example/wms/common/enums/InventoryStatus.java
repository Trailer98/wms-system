package com.example.wms.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum InventoryStatus {
    NORMAL("NORMAL", "正常"),
    EXCEPTION("EXCEPTION", "异常");

    @EnumValue
    private final String code;
    private final String label;

    InventoryStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }
}
