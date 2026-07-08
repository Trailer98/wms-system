package com.example.wms.admin.controller;

import com.example.wms.admin.annotation.RequiresPermission;
import com.example.wms.common.common.ApiResponse;
import com.example.wms.admin.service.InventoryService;
import com.example.wms.admin.view.dto.InventoryQuery;
import com.example.wms.admin.view.dto.InventoryResponse;
import com.example.wms.admin.view.dto.StockMovementQuery;
import com.example.wms.admin.view.dto.StockMovementResponse;
import com.example.wms.common.common.PageResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    @RequiresPermission("inventory:view")
    public ApiResponse<PageResponse<InventoryResponse>> search(InventoryQuery query) {
        return ApiResponse.ok(inventoryService.search(query));
    }

    @GetMapping("/{id}")
    @RequiresPermission("inventory:view")
    public ApiResponse<InventoryResponse> getDetail(@PathVariable Long id) {
        return ApiResponse.ok(inventoryService.getDetail(id));
    }

    @GetMapping("/transactions/page")
    @RequiresPermission("inventory:transaction:view")
    public ApiResponse<PageResponse<StockMovementResponse>> searchTransactions(StockMovementQuery query) {
        return ApiResponse.ok(inventoryService.searchTransactions(query));
    }
}
