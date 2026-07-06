package com.example.wms.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum AreaType {
    RECEIVING("RECEIVING", "收货区"),
    STORAGE("STORAGE", "存储区"),
    PICKING("PICKING", "拣货区"),
    QC("QC", "质检区"),
    REVIEW("REVIEW", "复核区"),
    SHIPPING("SHIPPING", "发货区"),
    EXCEPTION("EXCEPTION", "异常区"),
    FROZEN("FROZEN", "冻结区");

    @EnumValue
    private final String code;
    private final String label;

    AreaType(String code, String label) {
        this.code = code;
        this.label = label;
    }
}
