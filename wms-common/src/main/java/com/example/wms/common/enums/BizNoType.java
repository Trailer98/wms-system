package com.example.wms.common.enums;

import lombok.Getter;

@Getter
public enum BizNoType {
    INBOUND_ORDER("IN", "入库单"),
    OUTBOUND_ORDER("OUT", "出库单"),
    STOCK_ADJUST_ORDER("ADJ", "库存调整单"),
    STOCK_COUNT_TASK("CNT", "库存盘点单"),
    WMS_EXCEPTION("EXC", "异常事件");

    private final String prefix;
    private final String label;

    BizNoType(String prefix, String label) {
        this.prefix = prefix;
        this.label = label;
    }
}
