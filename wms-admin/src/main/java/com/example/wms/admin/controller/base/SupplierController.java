package com.example.wms.admin.controller.base;

import com.example.wms.admin.annotation.RequiresPermission;
import com.example.wms.admin.annotation.SysOperationLog;
import com.example.wms.common.common.ApiResponse;
import com.example.wms.admin.service.SupplierService;
import com.example.wms.admin.view.dto.CreateSupplierRequest;
import com.example.wms.admin.view.dto.SupplierQuery;
import com.example.wms.admin.view.dto.SupplierResponse;
import com.example.wms.admin.view.dto.UpdateSupplierEnabledRequest;
import com.example.wms.admin.view.dto.UpdateSupplierRequest;
import com.example.wms.common.common.PageResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/suppliers")
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @PostMapping
    @RequiresPermission("supplier:create")
    @SysOperationLog(operationType = "创建供应商", content = "创建供应商", module = "基础资料")
    public ApiResponse<SupplierResponse> create(@Valid @RequestBody CreateSupplierRequest request) {
        return ApiResponse.ok(supplierService.create(request));
    }

    @GetMapping
    @RequiresPermission("supplier:view")
    public ApiResponse<PageResponse<SupplierResponse>> search(SupplierQuery query) {
        return ApiResponse.ok(supplierService.search(query));
    }

    @PutMapping
    @RequiresPermission("supplier:update")
    @SysOperationLog(operationType = "修改供应商信息", content = "修改供应商信息", module = "基础资料")
    public ApiResponse<SupplierResponse> update(@Valid @RequestBody UpdateSupplierRequest request) {
        return ApiResponse.ok(supplierService.update(request));
    }

    @PatchMapping
    @RequiresPermission("supplier:disable")
    @SysOperationLog(operationType = "启停供应商", content = "启停供应商", module = "基础资料")
    public ApiResponse<Void> changeEnabled(@Valid @RequestBody UpdateSupplierEnabledRequest request) {
        supplierService.changeEnabled(request);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("supplier:delete")
    @SysOperationLog(operationType = "删除供应商", content = "删除供应商", module = "基础资料")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        supplierService.delete(id);
        return ApiResponse.ok();
    }
}
