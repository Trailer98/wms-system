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
import com.example.wms.common.enums.AdjustAction;
import com.example.wms.common.enums.AdjustReasonType;
import com.example.wms.common.enums.AdjustType;
import com.example.wms.common.enums.AreaType;
import com.example.wms.common.enums.BizNoType;
import com.example.wms.common.enums.HoldStatus;
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

    /**
     * Freezes source inventory for every pending transfer-to-exception item, so it can't be locked
     * away for outbound between submit and confirm. Restore-from-exception deliberately does nothing
     * to inventory here (the stock is already unavailable, sitting in an exception area — see
     * {@link #confirm}), and plain quantity items are unaffected, matching pre-existing behavior.
     */
    @Transactional
    public StockAdjustOrderResponse submit(Long id) {
        StockAdjustOrder order = getById(id);
        if (order.getStatus() != StockAdjustOrderStatus.DRAFT) {
            throw new BusinessException("adjust order can only be submitted from draft");
        }
        order.attachWarehouse(warehouseService.getById(order.getWarehouseId()));

        List<StockAdjustOrderItem> items = stockAdjustOrderItemMapper.selectList(Wrappers.lambdaQuery(StockAdjustOrderItem.class)
                .eq(StockAdjustOrderItem::getAdjustOrderId, order.getId()));
        for (StockAdjustOrderItem item : items) {
            if (item.getAdjustAction() == AdjustAction.TRANSFER_TO_EXCEPTION) {
                Inventory source = inventoryService.requireInventoryById(item.getInventoryId());
                inventoryService.hold(order.getWarehouse(), source.getArea(), source.getLocation(), source.getSku(),
                        item.getInventoryId(), item.getAdjustQty(), order.getAdjustNo(), "hold inventory pending transfer to exception area");
                item.markHeld(item.getAdjustQty());
                stockAdjustOrderItemMapper.updateById(item);
            }
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
     * decreaseOnHand}/{@code decreaseFrozenOnHand} themselves, not here.
     * <p>
     * TRANSFER_TO_EXCEPTION / RESTORE_FROM_EXCEPTION each write exactly two movements (an OUT at the
     * source, an IN at the destination) and never touch {@code inventory_finance_event} — there is no
     * finance-event service in this codebase yet, so there is nothing here that could generate one;
     * when one is eventually built it should simply never subscribe to these four operationTypes plus
     * STOCK_FREEZE/STOCK_UNFREEZE, since moving stock between areas (or holding/releasing it) never
     * changes the total asset quantity.
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

            WarehouseArea targetArea = null;
            WarehouseLocation targetLocation = null;
            if (isTransfer(item.getAdjustAction())) {
                targetArea = warehouseAreaService.getById(item.getTargetAreaId());
                targetLocation = warehouseLocationService.getById(item.getTargetLocationId());
                inventoryService.assertLocationUsable(targetLocation, sku, order.getWarehouse(), targetArea, order.getAdjustNo(), "adjust inventory transfer target");
            }
            resolved.add(new ResolvedItem(item, area, location, sku, targetArea, targetLocation));
        }

        for (ResolvedItem r : resolved) {
            StockMovement movement;
            switch (r.item().getAdjustAction()) {
                case QUANTITY_INCREASE -> {
                    movement = inventoryService.increaseOnHand(
                            order.getWarehouse(), r.area(), r.location(), r.sku(),
                            r.item().getAdjustQty(), MovementType.ADJUSTMENT, OperationType.STOCK_ADJUST_INCREASE,
                            order.getAdjustNo(), "stock adjust order increase"
                    );
                    if (r.item().getInventoryId() == null) {
                        Inventory landed = inventoryService.getByDimension(order.getWarehouseId(), r.sku().getId(), r.area().getId(), r.location().getId());
                        r.item().assignInventoryId(landed.getId());
                    }
                }
                case QUANTITY_DECREASE -> movement = inventoryService.decreaseOnHand(
                        order.getWarehouse(), r.area(), r.location(), r.sku(),
                        r.item().getAdjustQty(), MovementType.ADJUSTMENT, OperationType.STOCK_ADJUST_DECREASE,
                        order.getAdjustNo(), "stock adjust order decrease"
                );
                case TRANSFER_TO_EXCEPTION -> {
                    movement = inventoryService.decreaseFrozenOnHand(
                            order.getWarehouse(), r.area(), r.location(), r.sku(), r.item().getInventoryId(),
                            r.item().getAdjustQty(), MovementType.ADJUSTMENT, OperationType.TRANSFER_TO_EXCEPTION_OUT,
                            order.getAdjustNo(), "transfer to exception area - source"
                    );
                    inventoryService.increaseOnHand(
                            order.getWarehouse(), r.targetArea(), r.targetLocation(), r.sku(),
                            r.item().getAdjustQty(), MovementType.ADJUSTMENT, OperationType.TRANSFER_TO_EXCEPTION_IN,
                            order.getAdjustNo(), "transfer to exception area - target"
                    );
                    Inventory targetInventory = inventoryService.getByDimension(order.getWarehouseId(), r.sku().getId(), r.targetArea().getId(), r.targetLocation().getId());
                    r.item().assignTargetInventoryId(targetInventory.getId());
                    r.item().markConsumed();
                }
                case RESTORE_FROM_EXCEPTION -> {
                    movement = inventoryService.decreaseFrozenOnHand(
                            order.getWarehouse(), r.area(), r.location(), r.sku(), r.item().getInventoryId(),
                            r.item().getAdjustQty(), MovementType.ADJUSTMENT, OperationType.RESTORE_FROM_EXCEPTION_OUT,
                            order.getAdjustNo(), "restore from exception area - source"
                    );
                    inventoryService.increaseOnHand(
                            order.getWarehouse(), r.targetArea(), r.targetLocation(), r.sku(),
                            r.item().getAdjustQty(), MovementType.ADJUSTMENT, OperationType.RESTORE_FROM_EXCEPTION_IN,
                            order.getAdjustNo(), "restore from exception area - target"
                    );
                    Inventory targetInventory = inventoryService.getByDimension(order.getWarehouseId(), r.sku().getId(), r.targetArea().getId(), r.targetLocation().getId());
                    r.item().assignTargetInventoryId(targetInventory.getId());
                }
                default -> throw new IllegalStateException("unexpected adjust action: " + r.item().getAdjustAction());
            }
            r.item().recordMovement(movement);
            r.item().attachSku(r.sku());
            r.item().attachArea(r.area());
            r.item().attachLocation(r.location());
            if (r.targetArea() != null) {
                r.item().attachTargetArea(r.targetArea());
            }
            if (r.targetLocation() != null) {
                r.item().attachTargetLocation(r.targetLocation());
            }
            stockAdjustOrderItemMapper.updateById(r.item());
        }

        order.setItems(resolved.stream().map(ResolvedItem::item).toList());
        order.markCompleted(currentUsername());
        stockAdjustOrderMapper.updateById(order);
        return StockAdjustOrderResponse.from(order);
    }

    private record ResolvedItem(StockAdjustOrderItem item, WarehouseArea area, WarehouseLocation location, Sku sku,
            WarehouseArea targetArea, WarehouseLocation targetLocation) {
    }

    /**
     * Cancelling a submitted (but not yet confirmed) order that holds a pending transfer-to-exception
     * releases that hold — the stock goes back to fully available, with no transfer movement ever
     * written (only the hold/release pair) and no finance event (there is none to write in the first
     * place; see {@link #confirm}).
     */
    @Transactional
    public StockAdjustOrderResponse cancel(Long id, String reason) {
        StockAdjustOrder order = getById(id);
        if (order.getStatus() != StockAdjustOrderStatus.DRAFT && order.getStatus() != StockAdjustOrderStatus.SUBMITTED) {
            throw new BusinessException("adjust order cannot be cancelled in its current state");
        }

        if (order.getStatus() == StockAdjustOrderStatus.SUBMITTED) {
            order.attachWarehouse(warehouseService.getById(order.getWarehouseId()));
            List<StockAdjustOrderItem> items = stockAdjustOrderItemMapper.selectList(Wrappers.lambdaQuery(StockAdjustOrderItem.class)
                    .eq(StockAdjustOrderItem::getAdjustOrderId, order.getId()));
            for (StockAdjustOrderItem item : items) {
                if (item.getHoldStatus() == HoldStatus.HELD) {
                    Inventory source = inventoryService.requireInventoryById(item.getInventoryId());
                    inventoryService.releaseHold(order.getWarehouse(), source.getArea(), source.getLocation(), source.getSku(),
                            item.getInventoryId(), item.getHoldQty(), order.getAdjustNo(), "release hold on cancelled transfer");
                    item.markReleased();
                    stockAdjustOrderItemMapper.updateById(item);
                }
            }
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
     * Dispatches each item to the quantity path or the transfer path based on its resolved
     * {@link AdjustAction} (inferred from the legacy {@code adjustType} if the client didn't send one
     * — see {@link StockAdjustOrderItemRequest}). DAMAGE/QUALITY_ISSUE reasons are blocked from a
     * plain quantity decrease here, server-side, regardless of what the frontend does: physical stock
     * that is damaged or fails quality still exists, so it must move through
     * {@code TRANSFER_TO_EXCEPTION} rather than disappearing from the books directly.
     */
    private void addItems(StockAdjustOrder order, Warehouse warehouse, List<StockAdjustOrderItemRequest> itemRequests) {
        for (StockAdjustOrderItemRequest itemRequest : itemRequests) {
            AdjustAction action = resolveAdjustAction(itemRequest);
            if (isTransfer(action)) {
                addTransferItem(order, warehouse, itemRequest, action);
                continue;
            }
            if (action == AdjustAction.QUANTITY_DECREASE && isDamageOrQualityIssue(order.getReasonType())) {
                throw new BusinessException("DAMAGE/QUALITY_ISSUE reasons cannot use a plain quantity decrease; use TRANSFER_TO_EXCEPTION instead");
            }
            addQuantityItem(order, warehouse, itemRequest, action);
        }
    }

    private AdjustAction resolveAdjustAction(StockAdjustOrderItemRequest itemRequest) {
        if (itemRequest.adjustAction() != null) {
            return itemRequest.adjustAction();
        }
        return itemRequest.adjustType() == AdjustType.INCREASE ? AdjustAction.QUANTITY_INCREASE : AdjustAction.QUANTITY_DECREASE;
    }

    private boolean isTransfer(AdjustAction action) {
        return action == AdjustAction.TRANSFER_TO_EXCEPTION || action == AdjustAction.RESTORE_FROM_EXCEPTION;
    }

    private boolean isDamageOrQualityIssue(AdjustReasonType reasonType) {
        return reasonType == AdjustReasonType.DAMAGE || reasonType == AdjustReasonType.QUALITY_ISSUE;
    }

    /**
     * When {@code inventoryId} is given, the sku/warehouse/area/location come exclusively from that
     * inventory record — any skuId/areaId/locationId the client also sent is display-only and never
     * consulted. Without an inventoryId, this is an off-book increase: it must be explicitly opted
     * into (allowCreateInventory=true), can only increase, and needs its dimensions spelled out.
     */
    private void addQuantityItem(StockAdjustOrder order, Warehouse warehouse, StockAdjustOrderItemRequest itemRequest, AdjustAction action) {
        if (itemRequest.inventoryId() != null) {
            Inventory inventory = inventoryService.requireInventoryById(itemRequest.inventoryId());
            if (!inventory.getWarehouseId().equals(warehouse.getId())) {
                throw new BusinessException("selected inventory record does not belong to this adjust order's warehouse");
            }
            order.addItem(inventory.getId(), inventory.getSku(), inventory.getArea(), inventory.getLocation(),
                    itemRequest.adjustType(), action, itemRequest.adjustQty(), itemRequest.remark());
            return;
        }

        if (action != AdjustAction.QUANTITY_INCREASE) {
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
        order.addItem(null, sku, area, location, itemRequest.adjustType(), action, itemRequest.adjustQty(), itemRequest.remark());
    }

    /**
     * Validates and anchors a TRANSFER_TO_EXCEPTION/RESTORE_FROM_EXCEPTION item at create/update
     * time. Re-validated again at confirm time against fresh state (see {@link #confirm}) — this
     * pass exists so obviously-invalid transfers are rejected immediately instead of only surfacing
     * once the order is submitted and confirmed.
     */
    private void addTransferItem(StockAdjustOrder order, Warehouse warehouse, StockAdjustOrderItemRequest itemRequest, AdjustAction action) {
        if (itemRequest.inventoryId() == null) {
            throw new BusinessException("transfer/restore adjustments require inventoryId");
        }
        Inventory source = inventoryService.requireInventoryById(itemRequest.inventoryId());
        if (!source.getWarehouseId().equals(warehouse.getId())) {
            throw new BusinessException("selected inventory record does not belong to this adjust order's warehouse");
        }

        boolean toException = action == AdjustAction.TRANSFER_TO_EXCEPTION;
        AreaType sourceAreaType = source.getArea().getAreaType();
        if (toException && sourceAreaType == AreaType.EXCEPTION) {
            throw new BusinessException("source inventory is already in an exception area, cannot transfer to exception again");
        }
        if (!toException && sourceAreaType != AreaType.EXCEPTION) {
            throw new BusinessException("source inventory must be in an exception area to restore from it");
        }

        if (itemRequest.targetAreaId() == null || itemRequest.targetLocationId() == null) {
            throw new BusinessException("transfer/restore adjustments require targetAreaId and targetLocationId");
        }
        Long targetWarehouseId = itemRequest.targetWarehouseId() != null ? itemRequest.targetWarehouseId() : warehouse.getId();
        if (!targetWarehouseId.equals(warehouse.getId())) {
            throw new BusinessException("cross-warehouse transfer is not supported");
        }
        WarehouseArea targetArea = warehouseAreaService.getById(itemRequest.targetAreaId());
        if (!targetArea.getWarehouseId().equals(warehouse.getId())) {
            throw new BusinessException("target area does not belong to the given warehouse");
        }
        if (toException && targetArea.getAreaType() != AreaType.EXCEPTION) {
            throw new BusinessException("transfer-to-exception requires an EXCEPTION target area");
        }
        if (!toException && targetArea.getAreaType() == AreaType.EXCEPTION) {
            throw new BusinessException("restore-from-exception target area must not be an EXCEPTION area");
        }
        WarehouseLocation targetLocation = warehouseLocationService.getById(itemRequest.targetLocationId());
        if (!targetLocation.getAreaId().equals(targetArea.getId())) {
            throw new BusinessException("target location does not belong to the given target area");
        }
        if (!targetLocation.isUsable()) {
            throw new BusinessException("target location " + targetLocation.getLocationCode() + " is not usable (status=" + targetLocation.getStatus() + ")");
        }

        if (itemRequest.adjustQty() <= 0) {
            throw new BusinessException("adjust quantity must be greater than zero");
        }
        if (toException) {
            if (source.getAvailableQuantity() < itemRequest.adjustQty()) {
                throw new BusinessException("insufficient available quantity to transfer to exception area");
            }
        } else {
            if (source.getQuantity() < itemRequest.adjustQty()) {
                throw new BusinessException("insufficient on-hand quantity in exception area to restore");
            }
            if (source.getFrozenQuantity() < itemRequest.adjustQty()) {
                throw new BusinessException("insufficient frozen quantity in exception area to restore");
            }
        }

        order.addTransferItem(source.getId(), source.getSku(), source.getArea(), source.getLocation(),
                action, itemRequest.adjustQty(), itemRequest.remark(), targetWarehouseId, targetArea, targetLocation);
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
            if (item.getTargetAreaId() != null) {
                item.attachTargetArea(warehouseAreaService.getById(item.getTargetAreaId()));
            }
            if (item.getTargetLocationId() != null) {
                item.attachTargetLocation(warehouseLocationService.getById(item.getTargetLocationId()));
            }
        });
        order.setItems(items);
        return order;
    }

    private String currentUsername() {
        CurrentUser currentUser = CurrentUserContext.get();
        return currentUser != null ? currentUser.username() : "system";
    }
}
