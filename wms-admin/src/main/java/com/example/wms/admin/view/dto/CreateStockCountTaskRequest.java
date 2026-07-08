package com.example.wms.admin.view.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** {@code countNo} is accepted for backward compatibility but ignored: the system always mints its own number. */
public record CreateStockCountTaskRequest(
        @Size(max = 64) String countNo,
        @NotNull Long warehouseId,
        Long areaId,
        Long locationId,
        String remark
) {
}
