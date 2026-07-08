package com.example.wms.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum AdjustReasonType {
    DAMAGE("DAMAGE", "破损"),
    LOST("LOST", "丢失"),
    FOUND("FOUND", "找到库存"),
    DATA_ERROR("DATA_ERROR", "数据错误"),
    QUALITY_ISSUE("QUALITY_ISSUE", "质量异常"),
    COUNT_DIFF("COUNT_DIFF", "盘点差异"),
    OTHER("OTHER", "其他");

    @EnumValue
    private final String code;
    private final String label;

    AdjustReasonType(String code, String label) {
        this.code = code;
        this.label = label;
    }
}
