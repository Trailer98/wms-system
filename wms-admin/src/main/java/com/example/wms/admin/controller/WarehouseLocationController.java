package com.example.wms.admin.controller;

import com.example.wms.admin.annotation.SysOperationLog;
import com.example.wms.admin.service.WarehouseLocationService;
import com.example.wms.admin.view.dto.CreateWarehouseLocationRequest;
import com.example.wms.admin.view.dto.UpdateStatusRequest;
import com.example.wms.admin.view.dto.UpdateWarehouseLocationRequest;
import com.example.wms.admin.view.dto.WarehouseLocationQuery;
import com.example.wms.admin.view.dto.WarehouseLocationResponse;
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
@RequestMapping("/warehouse-locations")
public class WarehouseLocationController {

    private final WarehouseLocationService warehouseLocationService;

    public WarehouseLocationController(WarehouseLocationService warehouseLocationService) {
        this.warehouseLocationService = warehouseLocationService;
    }

    @PostMapping
    @SysOperationLog(operationType = "创建库位", content = "创建库位")
    public ApiResponse<WarehouseLocationResponse> create(@Valid @RequestBody CreateWarehouseLocationRequest request) {
        return ApiResponse.ok(warehouseLocationService.create(request));
    }

    @PutMapping("/{id}")
    @SysOperationLog(operationType = "编辑库位", content = "编辑库位")
    public ApiResponse<WarehouseLocationResponse> update(@PathVariable Long id, @Valid @RequestBody UpdateWarehouseLocationRequest request) {
        return ApiResponse.ok(warehouseLocationService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    @SysOperationLog(operationType = "切换库位状态", content = "切换库位状态")
    public ApiResponse<WarehouseLocationResponse> updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateStatusRequest request) {
        return ApiResponse.ok(warehouseLocationService.updateStatus(id, request));
    }

    @DeleteMapping("/{id}")
    @SysOperationLog(operationType = "删除库位", content = "删除库位")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        warehouseLocationService.delete(id);
        return ApiResponse.ok(null);
    }

    @GetMapping
    public ApiResponse<PageResponse<WarehouseLocationResponse>> search(WarehouseLocationQuery query) {
        return ApiResponse.ok(warehouseLocationService.search(query));
    }

    @GetMapping("/{id}")
    public ApiResponse<WarehouseLocationResponse> getDetail(@PathVariable Long id) {
        return ApiResponse.ok(warehouseLocationService.getDetail(id));
    }

    @GetMapping("/by-warehouse/{warehouseId}")
    public ApiResponse<List<WarehouseLocationResponse>> listByWarehouse(@PathVariable Long warehouseId) {
        return ApiResponse.ok(warehouseLocationService.listByWarehouse(warehouseId));
    }

    @GetMapping("/by-area/{areaId}")
    public ApiResponse<List<WarehouseLocationResponse>> listByArea(@PathVariable Long areaId) {
        return ApiResponse.ok(warehouseLocationService.listByArea(areaId));
    }
}
