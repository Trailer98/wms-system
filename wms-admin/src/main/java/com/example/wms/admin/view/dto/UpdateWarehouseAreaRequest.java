package com.example.wms.admin.view.dto;

import com.example.wms.common.enums.AreaType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateWarehouseAreaRequest(
        @NotBlank @Size(max = 128) String areaName,
        @NotNull AreaType areaType,
        int pickPriority,
        @Size(max = 255) String remark
) {
}
