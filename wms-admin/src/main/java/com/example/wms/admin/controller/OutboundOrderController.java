package com.example.wms.admin.controller;

import com.example.wms.admin.annotation.SysOperationLog;
import com.example.wms.common.common.ApiResponse;
import com.example.wms.admin.service.OutboundOrderService;
import com.example.wms.admin.view.dto.CreateOutboundOrderRequest;
import com.example.wms.admin.view.dto.OutboundOrderQuery;
import com.example.wms.admin.view.dto.OutboundOrderResponse;
import com.example.wms.common.common.PageResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/outbound-orders")
public class OutboundOrderController {

    private final OutboundOrderService outboundOrderService;

    public OutboundOrderController(OutboundOrderService outboundOrderService) {
        this.outboundOrderService = outboundOrderService;
    }

    @PostMapping
    @SysOperationLog(
            operationType = "创建出库单",
            content = "创建出库单",
            bizNo = "#request.orderNo()"
    )
    public ApiResponse<OutboundOrderResponse> create(@Valid @RequestBody CreateOutboundOrderRequest request) {
        return ApiResponse.ok(outboundOrderService.create(request));
    }

    @PostMapping("/{id}/ship")
    @SysOperationLog(
            operationType = "出库单发货",
            content = "出库单发货",
            bizNo = "#result.data().orderNo()"
    )
    public ApiResponse<OutboundOrderResponse> ship(@PathVariable Long id) {
        return ApiResponse.ok(outboundOrderService.ship(id));
    }

    @GetMapping
    public ApiResponse<PageResponse<OutboundOrderResponse>> search(OutboundOrderQuery query) {
        return ApiResponse.ok(outboundOrderService.search(query));
    }
}
