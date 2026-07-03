package com.example.wms.admin.controller;

import com.example.wms.admin.annotation.SysOperationLog;
import com.example.wms.common.common.ApiResponse;
import com.example.wms.admin.service.SupplierService;
import com.example.wms.admin.view.dto.CreateSupplierRequest;
import com.example.wms.admin.view.dto.SupplierQuery;
import com.example.wms.admin.view.dto.SupplierResponse;
import com.example.wms.common.common.PageResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/suppliers")
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @PostMapping
    @SysOperationLog(operationType = "创建供应商", content = "创建供应商")
    public ApiResponse<SupplierResponse> create(@Valid @RequestBody CreateSupplierRequest request) {
        return ApiResponse.ok(supplierService.create(request));
    }

    @GetMapping
    public ApiResponse<PageResponse<SupplierResponse>> search(SupplierQuery query) {
        return ApiResponse.ok(supplierService.search(query));
    }
}
