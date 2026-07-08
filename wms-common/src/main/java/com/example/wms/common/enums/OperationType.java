package com.example.wms.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum OperationType {
    INBOUND_RECEIVE("INBOUND_RECEIVE", "入库收货"),
    OUTBOUND_LOCK("OUTBOUND_LOCK", "出库锁库"),
    OUTBOUND_CANCEL_UNLOCK("OUTBOUND_CANCEL_UNLOCK", "出库取消解锁"),
    OUTBOUND_SHIP("OUTBOUND_SHIP", "出库发货"),
    STOCK_ADJUST_INCREASE("STOCK_ADJUST_INCREASE", "库存调整增加"),
    STOCK_ADJUST_DECREASE("STOCK_ADJUST_DECREASE", "库存调整减少"),
    STOCK_COUNT_PROFIT("STOCK_COUNT_PROFIT", "库存盘点盘盈"),
    STOCK_COUNT_LOSS("STOCK_COUNT_LOSS", "库存盘点盘亏"),
    STOCK_FREEZE("STOCK_FREEZE", "库存冻结"),
    STOCK_UNFREEZE("STOCK_UNFREEZE", "库存解冻"),
    TRANSFER_OUT("TRANSFER_OUT", "移库转出"),
    TRANSFER_IN("TRANSFER_IN", "移库转入"),
    TRANSFER_TO_EXCEPTION_OUT("TRANSFER_TO_EXCEPTION_OUT", "转异常区-转出"),
    TRANSFER_TO_EXCEPTION_IN("TRANSFER_TO_EXCEPTION_IN", "转异常区-转入"),
    RESTORE_FROM_EXCEPTION_OUT("RESTORE_FROM_EXCEPTION_OUT", "异常恢复-转出"),
    RESTORE_FROM_EXCEPTION_IN("RESTORE_FROM_EXCEPTION_IN", "异常恢复-转入"),
    UNKNOWN("UNKNOWN", "未知操作");

    @EnumValue
    private final String code;
    private final String label;

    OperationType(String code, String label) {
        this.code = code;
        this.label = label;
    }
}
