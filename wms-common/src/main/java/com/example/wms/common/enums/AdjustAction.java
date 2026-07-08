package com.example.wms.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * What a stock-adjust-order item actually does, superseding {@link AdjustType} as the source of
 * truth going forward (that older increase/decrease-only field is kept for backward compatibility,
 * see {@code stock_adjust_order_item.adjust_action} migration notes in schema.sql). A transfer
 * action still carries a legacy {@link AdjustType} of {@code DECREASE} (the source row's on-hand
 * drops either way), but {@code adjustAction} is what actually drives validation and inventory
 * mutation logic.
 */
@Getter
public enum AdjustAction {
    QUANTITY_INCREASE("QUANTITY_INCREASE", "数量调增"),
    QUANTITY_DECREASE("QUANTITY_DECREASE", "数量调减"),
    TRANSFER_TO_EXCEPTION("TRANSFER_TO_EXCEPTION", "转异常区"),
    RESTORE_FROM_EXCEPTION("RESTORE_FROM_EXCEPTION", "异常恢复正常");

    @EnumValue
    private final String code;
    private final String label;

    AdjustAction(String code, String label) {
        this.code = code;
        this.label = label;
    }
}
