package com.example.wms.admin.controller;

import com.example.wms.admin.annotation.RequiresPermission;
import com.example.wms.admin.annotation.SysOperationLog;
import com.example.wms.common.common.ApiResponse;
import com.example.wms.admin.service.CustomerService;
import com.example.wms.admin.view.dto.CreateCustomerRequest;
import com.example.wms.admin.view.dto.CustomerQuery;
import com.example.wms.admin.view.dto.CustomerResponse;
import com.example.wms.common.common.PageResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    @RequiresPermission("customer:create")
    @SysOperationLog(operationType = "创建客户", content = "创建客户", module = "基础资料")
    public ApiResponse<CustomerResponse> create(@Valid @RequestBody CreateCustomerRequest request) {
        return ApiResponse.ok(customerService.create(request));
    }

    @GetMapping
    @RequiresPermission("customer:view")
    public ApiResponse<PageResponse<CustomerResponse>> search(CustomerQuery query) {
        return ApiResponse.ok(customerService.search(query));
    }
}
