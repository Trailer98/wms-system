package com.example.wms.admin.view.dto;

import com.example.wms.admin.model.entity.StockCountTask;
import com.example.wms.common.enums.StockCountTaskStatus;

import java.time.Instant;
import java.util.List;

public record StockCountTaskResponse(
        Long id,
        String countNo,
        Long warehouseId,
        String warehouseCode,
        Long areaId,
        String areaCode,
        Long locationId,
        String locationCode,
        StockCountTaskStatus status,
        String remark,
        List<StockCountItemResponse> items,
        String createdBy,
        String completedBy,
        Instant completedAt,
        String cancelledBy,
        Instant cancelledAt,
        String cancelReason,
        Instant createdAt,
        Instant updatedAt
) {

    public static StockCountTaskResponse from(StockCountTask task) {
        return new StockCountTaskResponse(
                task.getId(),
                task.getCountNo(),
                task.getWarehouse().getId(),
                task.getWarehouse().getCode(),
                task.getArea() != null ? task.getArea().getId() : null,
                task.getArea() != null ? task.getArea().getAreaCode() : null,
                task.getLocation() != null ? task.getLocation().getId() : null,
                task.getLocation() != null ? task.getLocation().getLocationCode() : null,
                task.getStatus(),
                task.getRemark(),
                task.getItems().stream().map(StockCountItemResponse::from).toList(),
                task.getCreatedBy(),
                task.getCompletedBy(),
                task.getCompletedAt(),
                task.getCancelledBy(),
                task.getCancelledAt(),
                task.getCancelReason(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}
