package com.example.wms.admin.controller;

import com.example.wms.admin.annotation.SysOperationLog;
import com.example.wms.common.common.ApiResponse;
import com.example.wms.admin.service.SkuService;
import com.example.wms.admin.view.dto.CreateSkuRequest;
import com.example.wms.admin.view.dto.SkuResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/skus")
public class SkuController {

    private final SkuService skuService;

    public SkuController(SkuService skuService) {
        this.skuService = skuService;
    }

    @PostMapping
    @SysOperationLog(operationType = "创建SKU", content = "创建SKU")
    public ApiResponse<SkuResponse> create(@Valid @RequestBody CreateSkuRequest request) {
        return ApiResponse.ok(skuService.create(request));
    }

    @GetMapping
    @SysOperationLog(operationType = "查询SKU列表", content = "查询SKU列表")
    public ApiResponse<List<SkuResponse>> list() {
        return ApiResponse.ok(skuService.list());
    }
}
