package com.example.wms.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum CommonStatus {
    ENABLED("ENABLED", "启用"),
    DISABLED("DISABLED", "停用");

    @EnumValue
    private final String code;
    private final String label;

    CommonStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }
}
