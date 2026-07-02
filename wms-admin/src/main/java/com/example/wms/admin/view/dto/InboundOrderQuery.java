package com.example.wms.admin.view.dto;

import com.example.wms.common.common.PageRequest;
import com.example.wms.common.enums.InboundOrderStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InboundOrderQuery extends PageRequest {

    private String orderNo;
    private InboundOrderStatus status;
    private Long warehouseId;
}
