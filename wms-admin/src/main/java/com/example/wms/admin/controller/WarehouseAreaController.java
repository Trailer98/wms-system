package com.example.wms.admin.controller;

import com.example.wms.admin.annotation.SysOperationLog;
import com.example.wms.admin.service.WarehouseAreaService;
import com.example.wms.admin.view.dto.CreateWarehouseAreaRequest;
import com.example.wms.admin.view.dto.UpdateStatusRequest;
import com.example.wms.admin.view.dto.UpdateWarehouseAreaRequest;
import com.example.wms.admin.view.dto.WarehouseAreaQuery;
import com.example.wms.admin.view.dto.WarehouseAreaResponse;
import com.example.wms.common.common.ApiResponse;
import com.example.wms.common.common.PageResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/warehouse-areas")
public class WarehouseAreaController {

    private final WarehouseAreaService warehouseAreaService;

    public WarehouseAreaController(WarehouseAreaService warehouseAreaService) {
        this.warehouseAreaService = warehouseAreaService;
    }

    @PostMapping
    @SysOperationLog(operationType = "创建库区", content = "创建库区")
    public ApiResponse<WarehouseAreaResponse> create(@Valid @RequestBody CreateWarehouseAreaRequest request) {
        return ApiResponse.ok(warehouseAreaService.create(request));
    }

    @PutMapping("/{id}")
    @SysOperationLog(operationType = "编辑库区", content = "编辑库区")
    public ApiResponse<WarehouseAreaResponse> update(@PathVariable Long id, @Valid @RequestBody UpdateWarehouseAreaRequest request) {
        return ApiResponse.ok(warehouseAreaService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    @SysOperationLog(operationType = "切换库区状态", content = "切换库区状态")
    public ApiResponse<WarehouseAreaResponse> updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateStatusRequest request) {
        return ApiResponse.ok(warehouseAreaService.updateStatus(id, request));
    }

    @DeleteMapping("/{id}")
    @SysOperationLog(operationType = "删除库区", content = "删除库区")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        warehouseAreaService.delete(id);
        return ApiResponse.ok(null);
    }

    @GetMapping
    public ApiResponse<PageResponse<WarehouseAreaResponse>> search(WarehouseAreaQuery query) {
        return ApiResponse.ok(warehouseAreaService.search(query));
    }

    @GetMapping("/{id}")
    public ApiResponse<WarehouseAreaResponse> getDetail(@PathVariable Long id) {
        return ApiResponse.ok(warehouseAreaService.getDetail(id));
    }

    @GetMapping("/by-warehouse/{warehouseId}")
    public ApiResponse<List<WarehouseAreaResponse>> listByWarehouse(@PathVariable Long warehouseId) {
        return ApiResponse.ok(warehouseAreaService.listByWarehouse(warehouseId));
    }
}
