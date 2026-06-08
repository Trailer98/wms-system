package com.example.wms.admin.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.wms.common.common.BusinessException;
import com.example.wms.admin.model.entity.Inventory;
import com.example.wms.common.enums.MovementType;
import com.example.wms.admin.model.entity.Sku;
import com.example.wms.admin.model.entity.StockMovement;
import com.example.wms.admin.model.entity.Warehouse;
import com.example.wms.admin.model.mapper.InventoryMapper;
import com.example.wms.admin.model.mapper.StockMovementMapper;
import com.example.wms.admin.view.dto.InventoryResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InventoryService {

    private final InventoryMapper inventoryMapper;
    private final StockMovementMapper stockMovementMapper;
    private final WarehouseService warehouseService;
    private final SkuService skuService;

    public InventoryService(
            InventoryMapper inventoryMapper,
            StockMovementMapper stockMovementMapper,
            WarehouseService warehouseService,
            SkuService skuService
    ) {
        this.inventoryMapper = inventoryMapper;
        this.stockMovementMapper = stockMovementMapper;
        this.warehouseService = warehouseService;
        this.skuService = skuService;
    }

    @Transactional(readOnly = true)
    public List<InventoryResponse> search(Long warehouseId, Long skuId) {
        return inventoryMapper.selectList(Wrappers.lambdaQuery(Inventory.class)
                        .eq(warehouseId != null, Inventory::getWarehouseId, warehouseId)
                        .eq(skuId != null, Inventory::getSkuId, skuId)
                        .orderByAsc(Inventory::getWarehouseId, Inventory::getSkuId))
                .stream()
                .map(this::assemble)
                .map(InventoryResponse::from)
                .toList();
    }

    @Transactional
    public void receive(Warehouse warehouse, Sku sku, int quantity, String businessNo, String remark) {
        Inventory inventory = findByWarehouseAndSku(warehouse.getId(), sku.getId());
        if (inventory == null) {
            inventory = new Inventory(warehouse, sku);
            inventoryMapper.insert(inventory);
        }
        inventory.increase(quantity);
        inventoryMapper.updateById(inventory);
        stockMovementMapper.insert(new StockMovement(MovementType.INBOUND, warehouse, sku, quantity, businessNo, remark));
    }

    @Transactional
    public void ship(Warehouse warehouse, Sku sku, int quantity, String businessNo, String remark) {
        Inventory inventory = findByWarehouseAndSku(warehouse.getId(), sku.getId());
        if (inventory == null) {
            throw new BusinessException("inventory not found");
        }
        if (inventory.getAvailableQuantity() < quantity) {
            throw new BusinessException("available inventory is insufficient");
        }
        inventory.decrease(quantity);
        inventoryMapper.updateById(inventory);
        stockMovementMapper.insert(new StockMovement(MovementType.OUTBOUND, warehouse, sku, -quantity, businessNo, remark));
    }

    private Inventory findByWarehouseAndSku(Long warehouseId, Long skuId) {
        return inventoryMapper.selectOne(Wrappers.lambdaQuery(Inventory.class)
                .eq(Inventory::getWarehouseId, warehouseId)
                .eq(Inventory::getSkuId, skuId));
    }

    private Inventory assemble(Inventory inventory) {
        inventory.attachWarehouse(warehouseService.getById(inventory.getWarehouseId()));
        inventory.attachSku(skuService.getById(inventory.getSkuId()));
        return inventory;
    }
}
