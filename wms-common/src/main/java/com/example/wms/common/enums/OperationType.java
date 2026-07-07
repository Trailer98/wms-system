package com.example.wms.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum OperationType {
    ON_HAND_INCREASE("ON_HAND_INCREASE", "现存量增加"),
    ON_HAND_DECREASE("ON_HAND_DECREASE", "现存量减少"),
    STOCK_LOCK("STOCK_LOCK", "锁定库存"),
    STOCK_UNLOCK("STOCK_UNLOCK", "释放锁定库存"),
    STOCK_FREEZE("STOCK_FREEZE", "冻结库存"),
    STOCK_UNFREEZE("STOCK_UNFREEZE", "解冻库存"),
    ADJUST_INCREASE("ADJUST_INCREASE", "调整增加"),
    ADJUST_DECREASE("ADJUST_DECREASE", "调整减少"),
    COUNT_PROFIT("COUNT_PROFIT", "盘盈"),
    COUNT_LOSS("COUNT_LOSS", "盘亏"),
    TRANSFER_OUT("TRANSFER_OUT", "移库转出"),
    TRANSFER_IN("TRANSFER_IN", "移库转入"),
    UNKNOWN("UNKNOWN", "未知");

    @EnumValue
    private final String code;
    private final String label;

    OperationType(String code, String label) {
        this.code = code;
        this.label = label;
    }
}
