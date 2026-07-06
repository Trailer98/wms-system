package com.example.wms.admin.view.dto;

import com.example.wms.common.common.PageRequest;
import com.example.wms.common.enums.ExceptionType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class WmsExceptionEventQuery extends PageRequest {

    private ExceptionType exceptionType;
    private String bizNo;
    private Long skuId;
    private Long warehouseId;
    private Long areaId;
    private Long locationId;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
