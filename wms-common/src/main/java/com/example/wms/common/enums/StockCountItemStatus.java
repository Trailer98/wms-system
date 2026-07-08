package com.example.wms.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum StockCountItemStatus {
    PENDING("PENDING", "待盘点"),
    RECORDED("RECORDED", "已录入");

    @EnumValue
    private final String code;
    private final String label;

    StockCountItemStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }
}
