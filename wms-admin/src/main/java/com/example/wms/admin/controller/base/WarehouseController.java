package com.example.wms.admin.controller.base;

import com.example.wms.admin.annotation.RequiresPermission;
import com.example.wms.admin.annotation.SysOperationLog;
import com.example.wms.common.common.ApiResponse;
import com.example.wms.admin.service.WarehouseService;
import com.example.wms.admin.view.dto.CreateWarehouseRequest;
import com.example.wms.admin.view.dto.WarehouseQuery;
import com.example.wms.admin.view.dto.WarehouseResponse;
import com.example.wms.common.common.PageResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
