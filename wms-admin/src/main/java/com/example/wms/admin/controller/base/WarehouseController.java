package com.example.wms.admin.controller.base;

import com.example.wms.admin.annotation.RequiresPermission;
import com.example.wms.admin.annotation.SysOperationLog;
import com.example.wms.common.common.ApiResponse;
import com.example.wms.admin.service.WarehouseService;
import com.example.wms.admin.view.dto.CreateWarehouseRequest;
import com.example.wms.admin.view.dto.UpdateWarehouseEnabledRequest;
import com.example.wms.admin.view.dto.UpdateWarehouseRequest;
import com.example.wms.admin.view.dto.WarehouseQuery;
import com.example.wms.admin.view.dto.WarehouseResponse;
import com.example.wms.common.common.PageResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/warehouses")
public class WarehouseController {

    private final WarehouseService warehouseService;

    public WarehouseController(WarehouseService warehouseService) {
        this.warehouseService = warehouseService;
    }

    @PostMapping
    @RequiresPermission("warehouse:create")
    @SysOperationLog(operationType = "创建仓库", content = "创建仓库", module = "基础资料")
    public ApiResponse<WarehouseResponse> create(@Valid @RequestBody CreateWarehouseRequest request) {
        return ApiResponse.ok(warehouseService.create(request));
    }

    @GetMapping
    @RequiresPermission("warehouse:view")
    public ApiResponse<PageResponse<WarehouseResponse>> search(WarehouseQuery query) {
        return ApiResponse.ok(warehouseService.search(query));
    }

    @PutMapping
    @RequiresPermission("warehouse:update")
    @SysOperationLog(operationType = "修改仓库信息", content = "修改仓库信息", module = "基础资料")
    public ApiResponse<WarehouseResponse> update(@Valid @RequestBody UpdateWarehouseRequest request) {
        return ApiResponse.ok(warehouseService.update(request));
    }

    @PatchMapping
    @RequiresPermission("warehouse:disable")
    @SysOperationLog(operationType = "启停仓库", content = "启停仓库", module = "基础资料")
    public ApiResponse<Void> changeEnabled(@Valid @RequestBody UpdateWarehouseEnabledRequest request) {
        warehouseService.changeEnabled(request);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("warehouse:delete")
    @SysOperationLog(operationType = "删除仓库", content = "删除仓库", module = "基础资料")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        warehouseService.delete(id);
        return ApiResponse.ok();
    }
}
