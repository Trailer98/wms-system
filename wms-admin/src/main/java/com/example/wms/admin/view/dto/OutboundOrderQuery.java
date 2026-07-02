package com.example.wms.admin.view.dto;

import com.example.wms.common.common.PageRequest;
import com.example.wms.common.enums.OutboundOrderStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OutboundOrderQuery extends PageRequest {

    private String orderNo;
    private OutboundOrderStatus status;
    private Long warehouseId;
}
