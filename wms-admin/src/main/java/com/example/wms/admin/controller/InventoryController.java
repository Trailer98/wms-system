package com.example.wms.admin.controller;

import com.example.wms.common.common.ApiResponse;
import com.example.wms.admin.service.InventoryService;
import com.example.wms.admin.view.dto.InventoryResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public ApiResponse<List<InventoryResponse>> search(
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long skuId
    ) {
        return ApiResponse.ok(inventoryService.search(warehouseId, skuId));
    }
}
