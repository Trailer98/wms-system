package com.example.wms.admin.view.dto;

import com.example.wms.common.common.PageRequest;
import com.example.wms.common.enums.AdjustReasonType;
import com.example.wms.common.enums.StockAdjustOrderStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class StockAdjustOrderQuery extends PageRequest {

    private String adjustNo;
    private StockAdjustOrderStatus status;
    private AdjustReasonType reasonType;
    private Long warehouseId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
