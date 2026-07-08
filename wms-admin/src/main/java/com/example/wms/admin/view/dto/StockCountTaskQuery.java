package com.example.wms.admin.view.dto;

import com.example.wms.common.common.PageRequest;
import com.example.wms.common.enums.StockCountTaskStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class StockCountTaskQuery extends PageRequest {

    private String countNo;
    private Long warehouseId;
    private Long areaId;
    private Long locationId;
    private StockCountTaskStatus status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
