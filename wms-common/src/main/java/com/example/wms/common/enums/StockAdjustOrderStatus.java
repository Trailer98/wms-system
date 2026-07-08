package com.example.wms.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum StockAdjustOrderStatus {
    DRAFT("DRAFT", "草稿"),
    SUBMITTED("SUBMITTED", "已提交"),
    COMPLETED("COMPLETED", "已完成"),
    CANCELLED("CANCELLED", "已取消");

    @EnumValue
    private final String code;
    private final String label;

    StockAdjustOrderStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }
}
