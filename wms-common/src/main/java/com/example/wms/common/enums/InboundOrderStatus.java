package com.example.wms.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum InboundOrderStatus {
    CREATED("CREATED", "已创建"),
    RECEIVED("RECEIVED", "已收货");

    @EnumValue
    private final String code;
    private final String label;

    InboundOrderStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }
}
