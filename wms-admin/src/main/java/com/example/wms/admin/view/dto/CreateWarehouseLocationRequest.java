package com.example.wms.admin.view.dto;

import com.example.wms.common.enums.LocationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CreateWarehouseLocationRequest(
        @NotNull Long warehouseId,
        @NotNull Long areaId,
        @NotBlank @Size(max = 64) String locationCode,
        @Size(max = 128) String locationName,
        @NotNull LocationType locationType,
        @PositiveOrZero int capacityQty,
        boolean allowMixedSku,
        int pickPriority,
        @Size(max = 255) String remark
) {
}
