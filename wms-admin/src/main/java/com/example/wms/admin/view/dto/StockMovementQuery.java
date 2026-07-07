package com.example.wms.admin.view.dto;

import com.example.wms.common.common.PageRequest;
import com.example.wms.common.enums.MovementType;
import com.example.wms.common.enums.OperationType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class StockMovementQuery extends PageRequest {

    private Long skuId;
    private Long warehouseId;
    private Long areaId;
    private Long locationId;
    private MovementType type;
    private OperationType operationType;
    private String businessNo;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
