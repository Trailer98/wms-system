package com.example.wms.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum AreaStatus {
    ENABLED("ENABLED", "启用"),
    DISABLED("DISABLED", "停用");

    @EnumValue
    private final String code;
    private final String label;

    AreaStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }
}
