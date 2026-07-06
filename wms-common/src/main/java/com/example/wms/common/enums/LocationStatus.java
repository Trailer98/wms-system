package com.example.wms.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum LocationStatus {
    ENABLED("ENABLED", "启用"),
    DISABLED("DISABLED", "停用"),
    LOCKED("LOCKED", "锁定"),
    COUNTING("COUNTING", "盘点中"),
    FULL("FULL", "已满");

    @EnumValue
    private final String code;
    private final String label;

    LocationStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }
}
