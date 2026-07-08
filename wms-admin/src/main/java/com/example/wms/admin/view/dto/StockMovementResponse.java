package com.example.wms.admin.view.dto;

import com.example.wms.admin.model.entity.StockMovement;
import com.example.wms.admin.service.SysDictService;
import com.example.wms.common.enums.MovementType;
import com.example.wms.common.enums.OperationType;

import java.time.Instant;

/**
 * {@code type}/{@code operationType} remain the raw {@link MovementType}/{@link OperationType} enum
 * values driving business logic elsewhere — never removed, never reinterpreted here. {@code bizType}
 * is the same value as {@code type} restated as a plain string (this codebase's "业务类型" concept —
 * the frontend historically labeled {@code type} that way; there is no separate {@code biz_type}
 * column). {@code *Label}/{@code *TagType} are display-only, resolved through {@link SysDictService}
 * with a same-as-raw-code fallback baked in, so a missing/misconfigured dictionary entry can never
 * fail this response — see that service's class doc.
 */
public record StockMovementResponse(
        Long id,
        MovementType type,
        OperationType operationType,
        String bizType,
        String bizTypeLabel,
        String bizTypeTagType,
        String operationTypeLabel,
        String operationTypeTagType,
        String businessNo,
        Long skuId,
        String skuCode,
        String skuName,
        Long warehouseId,
        String warehouseCode,
        Long areaId,
        String areaCode,
        Long locationId,
        String locationCode,
        int quantityChange,
        Integer beforeQuantity,
        Integer afterQuantity,
        Integer beforeReservedQuantity,
        Integer afterReservedQuantity,
        Integer beforeFrozenQuantity,
        Integer afterFrozenQuantity,
        Integer beforeAvailableQuantity,
        Integer afterAvailableQuantity,
        String operator,
        Instant occurredAt,
        String remark
) {

    private static final String OPERATION_TYPE_DICT_CODE = "stock_movement_operation_type";
    private static final String BIZ_TYPE_DICT_CODE = "stock_movement_biz_type";

    public static StockMovementResponse from(StockMovement movement, SysDictService sysDictService) {
        String bizType = movement.getType().name();
        String operationType = movement.getOperationType().name();
        return new StockMovementResponse(
                movement.getId(),
                movement.getType(),
                movement.getOperationType(),
                bizType,
                sysDictService.getLabel(BIZ_TYPE_DICT_CODE, bizType),
                sysDictService.getTagType(BIZ_TYPE_DICT_CODE, bizType),
                sysDictService.getLabel(OPERATION_TYPE_DICT_CODE, operationType),
                sysDictService.getTagType(OPERATION_TYPE_DICT_CODE, operationType),
                movement.getBusinessNo(),
                movement.getSku().getId(),
                movement.getSku().getCode(),
                movement.getSku().getName(),
                movement.getWarehouse().getId(),
                movement.getWarehouse().getCode(),
                movement.getArea() != null ? movement.getArea().getId() : null,
                movement.getArea() != null ? movement.getArea().getAreaCode() : null,
                movement.getLocation() != null ? movement.getLocation().getId() : null,
                movement.getLocation() != null ? movement.getLocation().getLocationCode() : null,
                movement.getQuantityChange(),
                movement.getBeforeQuantity(),
                movement.getAfterQuantity(),
                movement.getBeforeReservedQuantity(),
                movement.getAfterReservedQuantity(),
                movement.getBeforeFrozenQuantity(),
                movement.getAfterFrozenQuantity(),
                movement.getBeforeAvailableQuantity(),
                movement.getAfterAvailableQuantity(),
                movement.getOperator(),
                movement.getOccurredAt(),
                movement.getRemark()
        );
    }
}
