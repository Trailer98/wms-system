package com.example.wms.admin.controller;

import com.example.wms.admin.annotation.RequiresPermission;
import com.example.wms.admin.annotation.SysOperationLog;
import com.example.wms.common.common.ApiResponse;
import com.example.wms.admin.service.OutboundOrderService;
import com.example.wms.admin.view.dto.CreateOutboundOrderRequest;
import com.example.wms.admin.view.dto.OutboundOrderQuery;
import com.example.wms.admin.view.dto.OutboundOrderResponse;
import com.example.wms.admin.view.dto.UpdateOutboundOrderRequest;
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
@RequestMapping("/outbound-orders")
public class OutboundOrderController {

    private final OutboundOrderService outboundOrderService;

    public OutboundOrderController(OutboundOrderService outboundOrderService) {
        this.outboundOrderService = outboundOrderService;
    }

    @PostMapping
    @RequiresPermission("outbound:create")
    @SysOperationLog(
            operationType = "创建出库单",
            content = "创建出库单",
            module = "出库管理",
            bizNo = "#request.orderNo()"
    )
    public ApiResponse<OutboundOrderResponse> create(@Valid @RequestBody CreateOutboundOrderRequest request) {
        return ApiResponse.ok(outboundOrderService.create(request));
    }

    @PutMapping("/{id}")
    @RequiresPermission("outbound:create")
    @SysOperationLog(
            operationType = "编辑出库单",
            content = "编辑出库单",
            module = "出库管理",
            bizNo = "#result.data().orderNo()"
    )
    public ApiResponse<OutboundOrderResponse> update(@PathVariable Long id, @Valid @RequestBody UpdateOutboundOrderRequest request) {
        return ApiResponse.ok(outboundOrderService.update(id, request));
    }

    @PostMapping("/{id}/lock")
    @RequiresPermission("outbound:lock")
    @SysOperationLog(
            operationType = "出库单锁库",
            content = "出库单锁库",
            module = "出库管理",
            bizNo = "#result.data().orderNo()"
    )
    public ApiResponse<OutboundOrderResponse> lock(@PathVariable Long id) {
        return ApiResponse.ok(outboundOrderService.lock(id));
    }

    @PostMapping("/{id}/ship")
    @RequiresPermission("outbound:confirm")
    @SysOperationLog(
            operationType = "出库单发货",
            content = "出库单发货",
            module = "出库管理",
            bizNo = "#result.data().orderNo()"
    )
    public ApiResponse<OutboundOrderResponse> ship(@PathVariable Long id) {
        return ApiResponse.ok(outboundOrderService.ship(id));
    }

    @PostMapping("/{id}/cancel")
    @RequiresPermission("outbound:cancel")
    @SysOperationLog(
            operationType = "出库单取消",
            content = "出库单取消",
            module = "出库管理",
            bizNo = "#result.data().orderNo()"
    )
    public ApiResponse<OutboundOrderResponse> cancel(@PathVariable Long id) {
        return ApiResponse.ok(outboundOrderService.cancel(id));
    }

    @GetMapping
    @RequiresPermission("outbound:view")
    public ApiResponse<PageResponse<OutboundOrderResponse>> search(OutboundOrderQuery query) {
        return ApiResponse.ok(outboundOrderService.search(query));
    }

    @GetMapping("/{id}")
    @RequiresPermission("outbound:view")
    public ApiResponse<OutboundOrderResponse> getDetail(@PathVariable Long id) {
        return ApiResponse.ok(outboundOrderService.getDetail(id));
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("outbound:cancel")
    @SysOperationLog(
            operationType = "删除出库单",
            content = "删除出库单",
            module = "出库管理"
    )
    public ApiResponse<Void> delete(@PathVariable Long id) {
        outboundOrderService.delete(id);
        return ApiResponse.ok(null);
    }
}
