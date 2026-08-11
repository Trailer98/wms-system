package com.example.wms.admin.controller.base;

import com.example.wms.admin.annotation.RequiresPermission;
import com.example.wms.admin.annotation.SysOperationLog;
import com.example.wms.common.common.ApiResponse;
import com.example.wms.admin.service.SkuService;
import com.example.wms.admin.view.dto.CreateSkuRequest;
import com.example.wms.admin.view.dto.SkuQuery;
import com.example.wms.admin.view.dto.SkuResponse;
import com.example.wms.admin.view.dto.UpdateSkuEnabledRequest;
import com.example.wms.admin.view.dto.UpdateSkuRequest;
import com.example.wms.common.common.PageResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/skus")
public class SkuController {

    private final SkuService skuService;

    public SkuController(SkuService skuService) {
        this.skuService = skuService;
    }

    @PostMapping
    @RequiresPermission("sku:create")
    @SysOperationLog(operationType = "创建SKU", content = "创建SKU", module = "基础资料")
    public ApiResponse<SkuResponse> create(@Valid @RequestBody CreateSkuRequest request) {
        return ApiResponse.ok(skuService.create(request));
    }

    @GetMapping
    @RequiresPermission("sku:view")
    public ApiResponse<PageResponse<SkuResponse>> search(SkuQuery query) {
        return ApiResponse.ok(skuService.search(query));
    }

    @PutMapping
    @RequiresPermission("sku:update")
    @SysOperationLog(operationType = "修改SKU信息", content = "修改SKU信息", module = "基础资料")
    public ApiResponse<SkuResponse> update(@Valid @RequestBody UpdateSkuRequest request) {
        return ApiResponse.ok(skuService.update(request));
    }

    @PatchMapping
    @RequiresPermission("sku:disable")
    @SysOperationLog(operationType = "启停SKU", content = "启停SKU", module = "基础资料")
    public ApiResponse<Void> changeEnabled(@Valid @RequestBody UpdateSkuEnabledRequest request) {
        skuService.changeEnabled(request);
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("sku:delete")
    @SysOperationLog(operationType = "删除SKU", content = "删除SKU", module = "基础资料")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        skuService.delete(id);
        return ApiResponse.ok();
    }
}
