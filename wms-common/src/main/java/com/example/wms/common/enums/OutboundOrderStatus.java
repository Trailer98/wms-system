package com.example.wms.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum OutboundOrderStatus {
    CREATED("CREATED", "已创建"),
    SHIPPED("SHIPPED", "已发货");

    @EnumValue
    private final String code;
    private final String label;

    OutboundOrderStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }
}
