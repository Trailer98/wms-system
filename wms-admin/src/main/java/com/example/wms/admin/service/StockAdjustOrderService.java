package com.example.wms.admin.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.wms.admin.model.entity.Inventory;
import com.example.wms.admin.model.entity.Sku;
import com.example.wms.admin.model.entity.StockAdjustOrder;
import com.example.wms.admin.model.entity.StockAdjustOrderItem;
import com.example.wms.admin.model.entity.StockMovement;
import com.example.wms.admin.model.entity.Warehouse;
import com.example.wms.admin.model.entity.WarehouseArea;
import com.example.wms.admin.model.entity.WarehouseLocation;
import com.example.wms.admin.model.mapper.StockAdjustOrderItemMapper;
import com.example.wms.admin.model.mapper.StockAdjustOrderMapper;
import com.example.wms.admin.security.CurrentUser;
import com.example.wms.admin.security.CurrentUserContext;
import com.example.wms.admin.view.dto.CreateStockAdjustOrderRequest;
import com.example.wms.admin.view.dto.StockAdjustOrderItemRequest;
import com.example.wms.admin.view.dto.StockAdjustOrderQuery;
import com.example.wms.admin.view.dto.StockAdjustOrderResponse;
import com.example.wms.admin.view.dto.UpdateStockAdjustOrderRequest;
import com.example.wms.common.common.BusinessException;
import com.example.wms.common.common.PageResponse;
import com.example.wms.common.enums.AdjustType;
import com.example.wms.common.enums.BizNoType;
import com.example.wms.common.enums.MovementType;
import com.example.wms.common.enums.OperationType;
import com.example.wms.common.enums.StockAdjustOrderStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
public class StockAdjustOrderService {

    private final StockAdjustOrderMapper stockAdjustOrderMapper;
    private final StockAdjustOrderItemMapper stockAdjustOrderItemMapper;
    private final WarehouseService warehouseService;
    private final SkuService skuService;
    private final WarehouseAreaService warehouseAreaService;
    private final WarehouseLocationService warehouseLocationService;
    private final InventoryService inventoryService;
    private final BizNoGeneratorService bizNoGeneratorService;

    public StockAdjustOrderService(
            StockAdjustOrderMapper stockAdjustOrderMapper,
            StockAdjustOrderItemMapper stockAdjustOrderItemMapper,
            WarehouseService warehouseService,
            SkuService skuService,
            WarehouseAreaService warehouseAreaService,
            WarehouseLocationService warehouseLocationService,
            InventoryService inventoryService,
            BizNoGeneratorService bizNoGeneratorService
    ) {
        this.stockAdjustOrderMapper = stockAdjustOrderMapper;
        this.stockAdjustOrderItemMapper = stockAdjustOrderItemMapper;
        this.warehouseService = warehouseService;
        this.skuService = skuService;
        this.warehouseAreaService = warehouseAreaService;
        this.warehouseLocationService = warehouseLocationService;
        this.inventoryService = inventoryService;
        this.bizNoGeneratorService = bizNoGeneratorService;
    }

    /** The system always mints its own adjust number; any {@code adjustNo} the client sends is ignored. */
    @Transactional
    public StockAdjustOrderResponse create(CreateStockAdjustOrderRequest request) {
        Warehouse warehouse = warehouseService.getById(request.warehouseId());
        StockAdjustOrder order = new StockAdjustOrder(bizNoGeneratorService.generate(BizNoType.STOCK_ADJUST_ORDER), warehouse, request.reasonType(), request.reason(), currentUsername());

        addItems(order, warehouse, request.items());

        stockAdjustOrderMapper.insert(order);
        order.getItems().forEach(item -> {
            item.assignOrderId(order.getId());
            stockAdjustOrderItemMapper.insert(item);
        });

        return StockAdjustOrderResponse.from(order);
    }

    @Transactional
    public StockAdjustOrderResponse update(Long id, UpdateStockAdjustOrderRequest request) {
        StockAdjustOrder order = getById(id);
        if (order.getStatus() != StockAdjustOrderStatus.DRAFT) {
            throw new BusinessException("adjust order can only be edited while in draft");
        }

        Warehouse warehouse = warehouseService.getById(order.getWarehouseId());
        order.update(request.reasonType(), request.reason());

        stockAdjustOrderItemMapper.delete(Wrappers.lambdaQuery(StockAdjustOrderItem.class)
                .eq(StockAdjustOrderItem::getAdjustOrderId, id));
        addItems(order, warehouse, request.items());
        order.getItems().forEach(stockAdjustOrderItemMapper::insert);

        stockAdjustOrderMapper.updateById(order);
        return getDetail(id);
    }

    @Transactional
    public StockAdjustOrderResponse submit(Long id) {
        StockAdjustOrder order = getById(id);
        if (order.getStatus() != StockAdjustOrderStatus.DRAFT) {
            throw new BusinessException("adjust order can only be submitted from draft");
        }
        order.markSubmitted();
        stockAdjustOrderMapper.updateById(order);
        return StockAdjustOrderResponse.from(assemble(order));
    }

    /**
     * Confirmation is the security-sensitive step: nothing cached at create/assemble time is
     * trusted. Every item is re-resolved against the database's current state in a first pass
     * (which is also where every validation failure surfaces, before any inventory is touched), and
     * only the fresh objects from that pass drive the actual mutation in a second pass.
     * <p>
     * This is the one caller of {@link InventoryService#increaseOnHand}/{@code decreaseOnHand} that
     * calls {@link InventoryService#assertLocationUsable} first — a plain stock adjustment must never
     * be allowed to touch a COUNTING (or DISABLED/LOCKED) location, unlike stock-count-task's own
     * completion, which is the one process allowed to write to a COUNTING location precisely because
     * finishing the count is what clears that status. The row-level locking and before/after snapshot
     * correctness under concurrent confirmations is handled inside {@code increaseOnHand}/{@code
     * decreaseOnHand} themselves, not here.
     */
    @Transactional
    public StockAdjustOrderResponse confirm(Long id) {
        StockAdjustOrder order = getById(id);
        if (order.getStatus() != StockAdjustOrderStatus.SUBMITTED) {
            throw new BusinessException("adjust order must be submitted before it can be confirmed");
        }
        order.attachWarehouse(warehouseService.getById(order.getWarehouseId()));

        List<StockAdjustOrderItem> items = stockAdjustOrderItemMapper.selectList(Wrappers.lambdaQuery(StockAdjustOrderItem.class)
                .eq(StockAdjustOrderItem::getAdjustOrderId, order.getId())
                .orderByAsc(StockAdjustOrderItem::getId));

        List<ResolvedItem> resolved = new ArrayList<>();
        for (StockAdjustOrderItem item : items) {
            Sku sku;
            WarehouseArea area;
            WarehouseLocation location;
            if (item.getInventoryId() != null) {
                Inventory inventory = inventoryService.requireInventoryById(item.getInventoryId());
                sku = inventory.getSku();
                area = inventory.getArea();
                location = inventory.getLocation();
            } else {
                sku = skuService.getById(item.getSkuId());
                area = warehouseAreaService.getById(item.getAreaId());
                location = warehouseLocationService.getById(item.getLocationId());
            }
            inventoryService.assertLocationUsable(location, sku, order.getWarehouse(), area, order.getAdjustNo(), "adjust inventory");
            resolved.add(new ResolvedItem(item, area, location, sku));
        }

        for (ResolvedItem r : resolved) {
            StockMovement movement;
            if (r.item().getAdjustType() == AdjustType.INCREASE) {
                movement = inventoryService.increaseOnHand(
                        order.getWarehouse(), r.area(), r.location(), r.sku(),
                        r.item().getAdjustQty(), MovementType.ADJUSTMENT, OperationType.STOCK_ADJUST_INCREASE,
                        order.getAdjustNo(), "stock adjust order increase"
                );
                if (r.item().getInventoryId() == null) {
                    Inventory landed = inventoryService.getByDimension(order.getWarehouseId(), r.sku().getId(), r.area().getId(), r.location().getId());
                    r.item().assignInventoryId(landed.getId());
                }
            } else {
                movement = inventoryService.decreaseOnHand(
                        order.getWarehouse(), r.area(), r.location(), r.sku(),
                        r.item().getAdjustQty(), MovementType.ADJUSTMENT, OperationType.STOCK_ADJUST_DECREASE,
                        order.getAdjustNo(), "stock adjust order decrease"
                );
            }
            r.item().recordMovement(movement);
            r.item().attachSku(r.sku());
            r.item().attachArea(r.area());
            r.item().attachLocation(r.location());
            stockAdjustOrderItemMapper.updateById(r.item());
        }

        order.setItems(resolved.stream().map(ResolvedItem::item).toList());
        order.markCompleted(currentUsername());
        stockAdjustOrderMapper.updateById(order);
        return StockAdjustOrderResponse.from(order);
    }

    private record ResolvedItem(StockAdjustOrderItem item, WarehouseArea area, WarehouseLocation location, Sku sku) {
    }

    @Transactional
    public StockAdjustOrderResponse cancel(Long id, String reason) {
        StockAdjustOrder order = getById(id);
        if (order.getStatus() != StockAdjustOrderStatus.DRAFT && order.getStatus() != StockAdjustOrderStatus.SUBMITTED) {
            throw new BusinessException("adjust order cannot be cancelled in its current state");
        }
        order.markCancelled(currentUsername(), reason);
        stockAdjustOrderMapper.updateById(order);
        return StockAdjustOrderResponse.from(assemble(order));
    }

    @Transactional(readOnly = true)
    public StockAdjustOrderResponse getDetail(Long id) {
        return StockAdjustOrderResponse.from(assemble(getById(id)));
    }

    @Transactional(readOnly = true)
    public PageResponse<StockAdjustOrderResponse> search(StockAdjustOrderQuery query) {
        Page<StockAdjustOrder> page = stockAdjustOrderMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                Wrappers.lambdaQuery(StockAdjustOrder.class)
                        .like(StringUtils.hasText(query.getAdjustNo()), StockAdjustOrder::getAdjustNo, query.getAdjustNo())
                        .eq(query.getStatus() != null, StockAdjustOrder::getStatus, query.getStatus())
                        .eq(query.getReasonType() != null, StockAdjustOrder::getReasonType, query.getReasonType())
                        .eq(query.getWarehouseId() != null, StockAdjustOrder::getWarehouseId, query.getWarehouseId())
                        .ge(query.getStartTime() != null, StockAdjustOrder::getCreatedAt, query.getStartTime())
                        .le(query.getEndTime() != null, StockAdjustOrder::getCreatedAt, query.getEndTime())
                        .orderByDesc(StockAdjustOrder::getCreatedAt)
        );

        return PageResponse.from(page, order -> StockAdjustOrderResponse.from(assemble(order)));
    }

    /**
     * When {@code inventoryId} is given, the sku/warehouse/area/location come exclusively from that
     * inventory record — any skuId/areaId/locationId the client also sent is display-only and never
     * consulted. Without an inventoryId, this is an off-book increase: it must be explicitly opted
     * into (allowCreateInventory=true), can only increase, and needs its dimensions spelled out.
     */
    private void addItems(StockAdjustOrder order, Warehouse warehouse, List<StockAdjustOrderItemRequest> itemRequests) {
        for (StockAdjustOrderItemRequest itemRequest : itemRequests) {
            if (itemRequest.inventoryId() != null) {
                Inventory inventory = inventoryService.requireInventoryById(itemRequest.inventoryId());
                if (!inventory.getWarehouseId().equals(warehouse.getId())) {
                    throw new BusinessException("selected inventory record does not belong to this adjust order's warehouse");
                }
                order.addItem(inventory.getId(), inventory.getSku(), inventory.getArea(), inventory.getLocation(),
                        itemRequest.adjustType(), itemRequest.adjustQty(), itemRequest.remark());
                continue;
            }

            if (itemRequest.adjustType() != AdjustType.INCREASE) {
                throw new BusinessException("an off-book adjustment (no inventoryId) only supports increase");
            }
            if (!itemRequest.allowCreateInventory()) {
                throw new BusinessException("off-book adjustment requires allowCreateInventory=true");
            }
            if (itemRequest.skuId() == null || itemRequest.areaId() == null || itemRequest.locationId() == null) {
                throw new BusinessException("off-book adjustment requires skuId, areaId and locationId");
            }

            Sku sku = skuService.getById(itemRequest.skuId());
            WarehouseArea area = warehouseAreaService.getById(itemRequest.areaId());
            if (!area.getWarehouseId().equals(warehouse.getId())) {
                throw new BusinessException("area does not belong to the given warehouse");
            }
            WarehouseLocation location = warehouseLocationService.getById(itemRequest.locationId());
            if (!location.getAreaId().equals(area.getId())) {
                throw new BusinessException("location does not belong to the given area");
            }
            if (!location.isUsable()) {
                throw new BusinessException("location " + location.getLocationCode() + " is not usable for off-book adjustment (status=" + location.getStatus() + ")");
            }
            order.addItem(null, sku, area, location, itemRequest.adjustType(), itemRequest.adjustQty(), itemRequest.remark());
        }
    }

    private StockAdjustOrder getById(Long id) {
        StockAdjustOrder order = stockAdjustOrderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException("stock adjust order not found");
        }
        return order;
    }

    private StockAdjustOrder assemble(StockAdjustOrder order) {
        order.attachWarehouse(warehouseService.getById(order.getWarehouseId()));
        List<StockAdjustOrderItem> items = stockAdjustOrderItemMapper.selectList(Wrappers.lambdaQuery(StockAdjustOrderItem.class)
                .eq(StockAdjustOrderItem::getAdjustOrderId, order.getId())
                .orderByAsc(StockAdjustOrderItem::getId));
        items.forEach(item -> {
            item.attachSku(skuService.getById(item.getSkuId()));
            item.attachArea(warehouseAreaService.getById(item.getAreaId()));
            item.attachLocation(warehouseLocationService.getById(item.getLocationId()));
        });
        order.setItems(items);
        return order;
    }

    private String currentUsername() {
        CurrentUser currentUser = CurrentUserContext.get();
        return currentUser != null ? currentUser.username() : "system";
    }
}
