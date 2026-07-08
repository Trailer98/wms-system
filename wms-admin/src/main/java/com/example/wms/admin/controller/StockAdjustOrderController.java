package com.example.wms.admin.controller;

import com.example.wms.admin.annotation.RequiresPermission;
import com.example.wms.admin.annotation.SysOperationLog;
import com.example.wms.admin.service.StockAdjustOrderService;
import com.example.wms.admin.view.dto.CancelReasonRequest;
import com.example.wms.admin.view.dto.CreateStockAdjustOrderRequest;
import com.example.wms.admin.view.dto.StockAdjustOrderQuery;
import com.example.wms.admin.view.dto.StockAdjustOrderResponse;
import com.example.wms.admin.view.dto.UpdateStockAdjustOrderRequest;
import com.example.wms.common.common.ApiResponse;
import com.example.wms.common.common.PageResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/stock-adjust-orders")
public class StockAdjustOrderController {

    private final StockAdjustOrderService stockAdjustOrderService;

    public StockAdjustOrderController(StockAdjustOrderService stockAdjustOrderService) {
        this.stockAdjustOrderService = stockAdjustOrderService;
    }

    @GetMapping("/page")
    @RequiresPermission("stock-adjust:view")
    public ApiResponse<PageResponse<StockAdjustOrderResponse>> search(StockAdjustOrderQuery query) {
        return ApiResponse.ok(stockAdjustOrderService.search(query));
    }

    @GetMapping("/{id}")
    @RequiresPermission("stock-adjust:view")
    public ApiResponse<StockAdjustOrderResponse> getDetail(@PathVariable Long id) {
        return ApiResponse.ok(stockAdjustOrderService.getDetail(id));
    }

    @PostMapping
    @RequiresPermission("stock-adjust:create")
    @SysOperationLog(operationType = "创建库存调整单", content = "创建库存调整单", module = "库存调整", bizNo = "#request.adjustNo()")
    public ApiResponse<StockAdjustOrderResponse> create(@Valid @RequestBody CreateStockAdjustOrderRequest request) {
        return ApiResponse.ok(stockAdjustOrderService.create(request));
    }

    @PutMapping("/{id}")
    @RequiresPermission("stock-adjust:update")
    @SysOperationLog(operationType = "编辑库存调整单", content = "编辑库存调整单", module = "库存调整")
    public ApiResponse<StockAdjustOrderResponse> update(@PathVariable Long id, @Valid @RequestBody UpdateStockAdjustOrderRequest request) {
        return ApiResponse.ok(stockAdjustOrderService.update(id, request));
    }

    @PostMapping("/{id}/submit")
    @RequiresPermission("stock-adjust:submit")
    @SysOperationLog(operationType = "提交库存调整单", content = "提交库存调整单", module = "库存调整")
    public ApiResponse<StockAdjustOrderResponse> submit(@PathVariable Long id) {
        return ApiResponse.ok(stockAdjustOrderService.submit(id));
    }

    @PostMapping("/{id}/confirm")
    @RequiresPermission("stock-adjust:confirm")
    @SysOperationLog(operationType = "确认库存调整单", content = "确认库存调整单", module = "库存调整")
    public ApiResponse<StockAdjustOrderResponse> confirm(@PathVariable Long id) {
        return ApiResponse.ok(stockAdjustOrderService.confirm(id));
    }

    @PostMapping("/{id}/cancel")
    @RequiresPermission("stock-adjust:cancel")
    @SysOperationLog(operationType = "取消库存调整单", content = "取消库存调整单", module = "库存调整")
    public ApiResponse<StockAdjustOrderResponse> cancel(@PathVariable Long id, @RequestBody(required = false) CancelReasonRequest request) {
        return ApiResponse.ok(stockAdjustOrderService.cancel(id, request != null ? request.reason() : null));
    }
}
