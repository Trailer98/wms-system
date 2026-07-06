package com.example.wms.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum LocationType {
    NORMAL("NORMAL", "普通库位"),
    TEMP("TEMP", "临时库位"),
    VIRTUAL("VIRTUAL", "虚拟库位");

    @EnumValue
    private final String code;
    private final String label;

    LocationType(String code, String label) {
        this.code = code;
        this.label = label;
    }
}
