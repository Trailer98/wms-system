package com.example.wms.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum MovementType {
    INBOUND("INBOUND", "入库"),
    OUTBOUND("OUTBOUND", "出库"),
    ADJUSTMENT("ADJUSTMENT", "库存调整");

    @EnumValue
    private final String code;
    private final String label;

    MovementType(String code, String label) {
        this.code = code;
        this.label = label;
    }
}
