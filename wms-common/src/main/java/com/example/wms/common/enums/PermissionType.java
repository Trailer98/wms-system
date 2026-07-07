package com.example.wms.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum PermissionType {
    MENU("MENU", "菜单"),
    BUTTON("BUTTON", "按钮"),
    API("API", "接口");

    @EnumValue
    private final String code;
    private final String label;

    PermissionType(String code, String label) {
        this.code = code;
        this.label = label;
    }
}
