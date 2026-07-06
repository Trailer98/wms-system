package com.example.wms.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum LockStatus {
    LOCKED("LOCKED", "已锁定"),
    RELEASED("RELEASED", "已释放"),
    SHIPPED("SHIPPED", "已出库");

    @EnumValue
    private final String code;
    private final String label;

    LockStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }
}
