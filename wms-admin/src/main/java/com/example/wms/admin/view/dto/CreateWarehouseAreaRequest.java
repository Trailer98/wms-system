package com.example.wms.admin.view.dto;

import com.example.wms.common.enums.AreaType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateWarehouseAreaRequest(
        @NotNull Long warehouseId,
        @NotBlank @Size(max = 64) String areaCode,
        @NotBlank @Size(max = 128) String areaName,
        @NotNull AreaType areaType,
        int pickPriority,
        @Size(max = 255) String remark
) {
}
