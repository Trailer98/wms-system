package com.example.wms.admin.controller.base;

import com.example.wms.admin.annotation.RequiresPermission;
import com.example.wms.admin.annotation.SysOperationLog;
import com.example.wms.common.common.ApiResponse;
import com.example.wms.admin.service.SkuService;
import com.example.wms.admin.view.dto.CreateSkuRequest;
import com.example.wms.admin.view.dto.SkuQuery;
import com.example.wms.admin.view.dto.SkuResponse;
import com.example.wms.common.common.PageResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
