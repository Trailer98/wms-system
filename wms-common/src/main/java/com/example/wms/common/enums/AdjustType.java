package com.example.wms.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum AdjustType {
    INCREASE("INCREASE", "调增"),
    DECREASE("DECREASE", "调减");

    @EnumValue
    private final String code;
    private final String label;

    AdjustType(String code, String label) {
        this.code = code;
        this.label = label;
    }
}
