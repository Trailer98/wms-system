package com.example.wms.admin.controller.base;

import com.example.wms.admin.annotation.RequiresPermission;
import com.example.wms.admin.annotation.SysOperationLog;
import com.example.wms.admin.view.dto.base.customer.UpdateCustomerRequest;
import com.example.wms.admin.view.dto.base.customer.UpdateEnabledRequest;
import com.example.wms.common.common.ApiResponse;
import com.example.wms.admin.service.CustomerService;
import com.example.wms.admin.view.dto.base.customer.CreateCustomerRequest;
import com.example.wms.admin.view.dto.base.customer.CustomerQuery;
import com.example.wms.admin.view.dto.base.customer.CustomerResponse;
import com.example.wms.common.common.PageResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

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

    @PutMapping
    @RequiresPermission("customer:update")
    @SysOperationLog(operationType = "修改客户信息", content = "修改客户信息", module = "基础资料")
    public ApiResponse<CustomerResponse> getCustomerById(@Valid @RequestBody UpdateCustomerRequest request) {
        return ApiResponse.ok(customerService.editCustomer(request));
    }

    @PatchMapping
    @RequiresPermission("customer:disable")
    @SysOperationLog(operationType = "启停客户", content = "启停客户", module = "基础资料")
    public ApiResponse<Void> changeEnabled(@Valid @RequestBody UpdateEnabledRequest request) {
        customerService.changeEnabled(request);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("customer:delete")
    @SysOperationLog(operationType = "删除客户", content = "删除客户", module = "基础资料")
    public ApiResponse<Void> deleteCustomer(@PathVariable Long id) {
        customerService.deleteCustomer(id);
        return ApiResponse.ok();
    }
}
