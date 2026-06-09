package com.example.wms.admin.controller;

import com.example.wms.admin.annotation.SysOperationLog;
import com.example.wms.common.common.ApiResponse;
import com.example.wms.admin.service.WarehouseService;
import com.example.wms.admin.view.dto.CreateWarehouseRequest;
import com.example.wms.admin.view.dto.WarehouseResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/warehouses")
public class WarehouseController {

    private final WarehouseService warehouseService;

    public WarehouseController(WarehouseService warehouseService) {
        this.warehouseService = warehouseService;
    }

    @PostMapping
    @SysOperationLog(operationType = "创建仓库", content = "创建仓库")
    public ApiResponse<WarehouseResponse> create(@Valid @RequestBody CreateWarehouseRequest request) {
        return ApiResponse.ok(warehouseService.create(request));
    }

    @GetMapping
    @SysOperationLog(operationType = "查询仓库列表", content = "查询仓库列表")
    public ApiResponse<List<WarehouseResponse>> list() {
        return ApiResponse.ok(warehouseService.list());
    }
}
