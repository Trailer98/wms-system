package com.example.wms.admin.controller;

import com.example.wms.admin.annotation.RequiresPermission;
import com.example.wms.admin.annotation.SysOperationLog;
import com.example.wms.common.common.ApiResponse;
import com.example.wms.admin.service.InboundOrderService;
import com.example.wms.admin.view.dto.CreateInboundOrderRequest;
import com.example.wms.admin.view.dto.InboundOrderQuery;
import com.example.wms.admin.view.dto.InboundOrderResponse;
import com.example.wms.admin.view.dto.UpdateInboundOrderRequest;
import com.example.wms.common.common.PageResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/inbound-orders")
public class InboundOrderController {

    private final InboundOrderService inboundOrderService;

    public InboundOrderController(InboundOrderService inboundOrderService) {
        this.inboundOrderService = inboundOrderService;
    }

    @PostMapping
    @RequiresPermission("inbound:create")
    @SysOperationLog(
            operationType = "创建入库单",
            content = "创建入库单",
            module = "入库管理",
            bizType = "INBOUND_ORDER",
            bizNo = "#result.data().orderNo()",
            bizId = "#result.data().id()"
    )
    public ApiResponse<InboundOrderResponse> create(@Valid @RequestBody CreateInboundOrderRequest request) {
        return ApiResponse.ok(inboundOrderService.create(request));
    }

    @PutMapping("/{id}")
    @RequiresPermission("inbound:update")
    @SysOperationLog(
            operationType = "编辑入库单",
            content = "编辑入库单",
            module = "入库管理",
            bizType = "INBOUND_ORDER",
            bizNo = "#result.data().orderNo()",
            bizId = "#result.data().id()"
    )
    public ApiResponse<InboundOrderResponse> update(@PathVariable Long id, @Valid @RequestBody UpdateInboundOrderRequest request) {
        return ApiResponse.ok(inboundOrderService.update(id, request));
    }

    @PostMapping("/{id}/receive")
    @RequiresPermission("inbound:complete")
    @SysOperationLog(
            operationType = "入库单收货",
            content = "入库单收货",
            module = "入库管理",
            bizType = "INBOUND_ORDER",
            bizNo = "#result.data().orderNo()",
            bizId = "#result.data().id()"
    )
    public ApiResponse<InboundOrderResponse> receive(@PathVariable Long id) {
        return ApiResponse.ok(inboundOrderService.receive(id));
    }

    @GetMapping
    @RequiresPermission("inbound:view")
    public ApiResponse<PageResponse<InboundOrderResponse>> search(InboundOrderQuery query) {
        return ApiResponse.ok(inboundOrderService.search(query));
    }

    @GetMapping("/{id}")
    @RequiresPermission("inbound:view")
    public ApiResponse<InboundOrderResponse> getDetail(@PathVariable Long id) {
        return ApiResponse.ok(inboundOrderService.getDetail(id));
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("inbound:cancel")
    @SysOperationLog(
            operationType = "删除入库单",
            content = "删除入库单",
            module = "入库管理"
    )
    public ApiResponse<Void> delete(@PathVariable Long id) {
        inboundOrderService.delete(id);
        return ApiResponse.ok(null);
    }
}
