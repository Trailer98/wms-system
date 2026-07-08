package com.example.wms.admin.controller;

import com.example.wms.admin.annotation.RequiresPermission;
import com.example.wms.admin.annotation.SysOperationLog;
import com.example.wms.admin.service.StockCountTaskService;
import com.example.wms.admin.view.dto.CancelReasonRequest;
import com.example.wms.admin.view.dto.CreateStockCountTaskRequest;
import com.example.wms.admin.view.dto.StockCountTaskQuery;
import com.example.wms.admin.view.dto.StockCountTaskResponse;
import com.example.wms.admin.view.dto.UpdateStockCountItemsRequest;
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
@RequestMapping("/stock-count-tasks")
public class StockCountTaskController {

    private final StockCountTaskService stockCountTaskService;

    public StockCountTaskController(StockCountTaskService stockCountTaskService) {
        this.stockCountTaskService = stockCountTaskService;
    }

    @GetMapping("/page")
    @RequiresPermission("stock-count:view")
    public ApiResponse<PageResponse<StockCountTaskResponse>> search(StockCountTaskQuery query) {
        return ApiResponse.ok(stockCountTaskService.search(query));
    }

    @GetMapping("/{id}")
    @RequiresPermission("stock-count:view")
    public ApiResponse<StockCountTaskResponse> getDetail(@PathVariable Long id) {
        return ApiResponse.ok(stockCountTaskService.getDetail(id));
    }

    @PostMapping
    @RequiresPermission("stock-count:create")
    @SysOperationLog(operationType = "创建库存盘点任务", content = "创建库存盘点任务", module = "库存盘点", bizNo = "#request.countNo()")
    public ApiResponse<StockCountTaskResponse> create(@Valid @RequestBody CreateStockCountTaskRequest request) {
        return ApiResponse.ok(stockCountTaskService.create(request));
    }

    @PostMapping("/{id}/start")
    @RequiresPermission("stock-count:start")
    @SysOperationLog(operationType = "开始库存盘点", content = "开始库存盘点", module = "库存盘点")
    public ApiResponse<StockCountTaskResponse> start(@PathVariable Long id) {
        return ApiResponse.ok(stockCountTaskService.start(id));
    }

    @PutMapping("/{id}/items")
    @RequiresPermission("stock-count:record")
    @SysOperationLog(operationType = "录入盘点实盘数量", content = "录入盘点实盘数量", module = "库存盘点")
    public ApiResponse<StockCountTaskResponse> record(@PathVariable Long id, @Valid @RequestBody UpdateStockCountItemsRequest request) {
        return ApiResponse.ok(stockCountTaskService.record(id, request));
    }

    @PostMapping("/{id}/complete")
    @RequiresPermission("stock-count:complete")
    @SysOperationLog(operationType = "完成库存盘点", content = "完成库存盘点", module = "库存盘点")
    public ApiResponse<StockCountTaskResponse> complete(@PathVariable Long id) {
        return ApiResponse.ok(stockCountTaskService.complete(id));
    }

    @PostMapping("/{id}/cancel")
    @RequiresPermission("stock-count:cancel")
    @SysOperationLog(operationType = "取消库存盘点", content = "取消库存盘点", module = "库存盘点")
    public ApiResponse<StockCountTaskResponse> cancel(@PathVariable Long id, @RequestBody(required = false) CancelReasonRequest request) {
        return ApiResponse.ok(stockCountTaskService.cancel(id, request != null ? request.reason() : null));
    }
}
