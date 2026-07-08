package com.example.wms.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * Tracks a stock-adjust-order item's hold on source inventory between submit and confirm. Only
 * {@link com.example.wms.common.enums.AdjustAction#TRANSFER_TO_EXCEPTION} items ever leave
 * {@code NONE}: submit freezes the source quantity (HELD), confirm consumes the hold (CONSUMED),
 * and cancelling a submitted-but-not-yet-confirmed transfer releases it (RELEASED).
 */
@Getter
public enum HoldStatus {
    NONE("NONE", "无冻结"),
    HELD("HELD", "已冻结"),
    RELEASED("RELEASED", "已释放"),
    CONSUMED("CONSUMED", "已消耗");

    @EnumValue
    private final String code;
    private final String label;

    HoldStatus(String code, String label) {
        this.code = code;
        this.label = label;
    }
}
