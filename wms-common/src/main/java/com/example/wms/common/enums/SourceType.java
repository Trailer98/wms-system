package com.example.wms.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum SourceType {
    MANUAL("MANUAL", "手工创建"),
    ERP("ERP", "ERP系统"),
    OMS("OMS", "订单管理系统"),
    API("API", "开放接口"),
    IMPORT("IMPORT", "批量导入"),
    SYSTEM("SYSTEM", "系统生成");

    @EnumValue
    private final String code;
    private final String label;

    SourceType(String code, String label) {
        this.code = code;
        this.label = label;
    }
}
