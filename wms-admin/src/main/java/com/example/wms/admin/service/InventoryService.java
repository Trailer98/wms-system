package com.example.wms.admin.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.wms.admin.model.entity.Inventory;
import com.example.wms.admin.model.entity.InventorySnapshot;
import com.example.wms.admin.model.entity.OutboundStockLock;
import com.example.wms.admin.model.entity.Sku;
import com.example.wms.admin.model.entity.StockMovement;
import com.example.wms.admin.model.entity.Warehouse;
import com.example.wms.admin.model.entity.WarehouseArea;
import com.example.wms.admin.model.entity.WarehouseLocation;
import com.example.wms.admin.model.mapper.InventoryMapper;
import com.example.wms.admin.model.mapper.OutboundStockLockMapper;
import com.example.wms.admin.model.mapper.SkuMapper;
import com.example.wms.admin.model.mapper.StockMovementMapper;
import com.example.wms.admin.model.mapper.WarehouseLocationMapper;
import com.example.wms.admin.view.dto.InventoryQuery;
import com.example.wms.admin.view.dto.InventoryResponse;
import com.example.wms.admin.view.dto.StockMovementQuery;
import com.example.wms.admin.view.dto.StockMovementResponse;
import com.example.wms.common.common.BusinessException;
import com.example.wms.common.common.PageResponse;
import com.example.wms.common.enums.AreaType;
import com.example.wms.common.enums.ExceptionType;
import com.example.wms.common.enums.InventoryStatus;
import com.example.wms.common.enums.LockStatus;
import com.example.wms.common.enums.MovementType;
import com.example.wms.common.enums.OperationType;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class InventoryService {

    private final InventoryMapper inventoryMapper;
    private final StockMovementMapper stockMovementMapper;
    private final WarehouseLocationMapper warehouseLocationMapper;
    private final OutboundStockLockMapper outboundStockLockMapper;
    private final SkuMapper skuMapper;
    private final WarehouseService warehouseService;
    private final SkuService skuService;
    private final WarehouseAreaService warehouseAreaService;
    private final WarehouseLocationService warehouseLocationService;
    private final WmsExceptionEventService wmsExceptionEventService;

    public InventoryService(
            InventoryMapper inventoryMapper,
            StockMovementMapper stockMovementMapper,
            WarehouseLocationMapper warehouseLocationMapper,
            OutboundStockLockMapper outboundStockLockMapper,
            SkuMapper skuMapper,
            WarehouseService warehouseService,
            SkuService skuService,
            WarehouseAreaService warehouseAreaService,
            WarehouseLocationService warehouseLocationService,
            WmsExceptionEventService wmsExceptionEventService
    ) {
        this.inventoryMapper = inventoryMapper;
        this.stockMovementMapper = stockMovementMapper;
        this.warehouseLocationMapper = warehouseLocationMapper;
        this.outboundStockLockMapper = outboundStockLockMapper;
        this.skuMapper = skuMapper;
        this.warehouseService = warehouseService;
        this.skuService = skuService;
        this.warehouseAreaService = warehouseAreaService;
        this.warehouseLocationService = warehouseLocationService;
        this.wmsExceptionEventService = wmsExceptionEventService;
    }

    @Transactional(readOnly = true)
    public PageResponse<InventoryResponse> search(InventoryQuery query) {
        List<Long> skuIds = null;
        if (StringUtils.hasText(query.getSkuCode())) {
            skuIds = skuMapper.selectList(Wrappers.lambdaQuery(Sku.class).like(Sku::getCode, query.getSkuCode()))
                    .stream().map(Sku::getId).toList();
            if (skuIds.isEmpty()) {
                Page<Inventory> emptyPage = new Page<>(query.getPageNum(), query.getPageSize());
                emptyPage.setRecords(List.of());
                emptyPage.setTotal(0);
                return PageResponse.from(emptyPage, inventory -> InventoryResponse.from(assemble(inventory)));
            }
        }

        List<Long> skuCodeMatches = skuIds;
        Page<Inventory> page = inventoryMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                Wrappers.lambdaQuery(Inventory.class)
                        .eq(query.getWarehouseId() != null, Inventory::getWarehouseId, query.getWarehouseId())
                        .eq(query.getSkuId() != null, Inventory::getSkuId, query.getSkuId())
                        .in(skuCodeMatches != null, Inventory::getSkuId, skuCodeMatches)
                        .eq(query.getAreaId() != null, Inventory::getAreaId, query.getAreaId())
                        .eq(query.getLocationId() != null, Inventory::getLocationId, query.getLocationId())
                        .eq(query.getInventoryStatus() != null, Inventory::getInventoryStatus, query.getInventoryStatus())
                        .gt(Boolean.TRUE.equals(query.getHasStock()), Inventory::getQuantity, 0)
                        .apply(Boolean.TRUE.equals(query.getOnlyAvailable()), "quantity - reserved_quantity - frozen_quantity > 0")
                        .orderByAsc(Inventory::getWarehouseId, Inventory::getSkuId)
        );

        return PageResponse.from(page, inventory -> InventoryResponse.from(assemble(inventory)));
    }

    @Transactional(readOnly = true)
    public InventoryResponse getDetail(Long id) {
        return InventoryResponse.from(requireInventoryById(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<StockMovementResponse> searchTransactions(StockMovementQuery query) {
        Page<StockMovement> page = stockMovementMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                Wrappers.lambdaQuery(StockMovement.class)
                        .eq(query.getSkuId() != null, StockMovement::getSkuId, query.getSkuId())
                        .eq(query.getWarehouseId() != null, StockMovement::getWarehouseId, query.getWarehouseId())
                        .eq(query.getAreaId() != null, StockMovement::getAreaId, query.getAreaId())
                        .eq(query.getLocationId() != null, StockMovement::getLocationId, query.getLocationId())
                        .eq(query.getType() != null, StockMovement::getType, query.getType())
                        .eq(query.getOperationType() != null, StockMovement::getOperationType, query.getOperationType())
                        .like(StringUtils.hasText(query.getBusinessNo()), StockMovement::getBusinessNo, query.getBusinessNo())
                        .ge(query.getStartTime() != null, StockMovement::getOccurredAt, query.getStartTime())
                        .le(query.getEndTime() != null, StockMovement::getOccurredAt, query.getEndTime())
                        .orderByDesc(StockMovement::getOccurredAt)
        );

        return PageResponse.from(page, movement -> StockMovementResponse.from(assembleMovement(movement)));
    }

    @Transactional
    public void receive(Warehouse warehouse, WarehouseArea area, WarehouseLocation location, Sku sku, int quantity, String businessNo, String remark) {
        assertLocationUsable(location, sku, warehouse, area, businessNo, "receive inbound");

        long conflictingSkuCount = inventoryMapper.selectCount(Wrappers.lambdaQuery(Inventory.class)
                .eq(Inventory::getLocationId, location.getId())
                .ne(Inventory::getSkuId, sku.getId())
                .gt(Inventory::getQuantity, 0));
        if (!location.isAllowMixedSku() && conflictingSkuCount > 0) {
            wmsExceptionEventService.record(ExceptionType.MIXED_SKU_NOT_ALLOWED, businessNo, sku.getId(), warehouse.getId(), area.getId(), location.getId(),
                    "location " + location.getLocationCode() + " already holds another sku and does not allow mixed sku");
            throw new BusinessException("location " + location.getLocationCode() + " does not allow mixed sku");
        }

        int occupied = warehouseLocationMapper.tryOccupy(location.getId(), quantity);
        if (occupied == 0) {
            wmsExceptionEventService.record(ExceptionType.LOCATION_CAPACITY_NOT_ENOUGH, businessNo, sku.getId(), warehouse.getId(), area.getId(), location.getId(),
                    "location " + location.getLocationCode() + " capacity is insufficient for quantity " + quantity);
            throw new BusinessException("location " + location.getLocationCode() + " capacity is insufficient");
        }

        Inventory inventory = findOrCreateInventory(warehouse, area, location, sku);
        InventorySnapshot before = inventory.snapshot();

        inventory.increase(quantity);
        if (area.getAreaType() == AreaType.EXCEPTION) {
            inventory.freeze(quantity);
        }
        inventoryMapper.updateById(inventory);
        InventorySnapshot after = inventory.snapshot();

        stockMovementMapper.insert(new StockMovement(
                MovementType.INBOUND, OperationType.INBOUND_RECEIVE, warehouse, area, location, sku,
                quantity, before, after,
                businessNo, remark
        ));
    }

    /**
     * Shared by stock-adjust-order confirm and stock-count-task complete: both need "increase
     * on-hand at a location and write a movement", differing only in the business movement/operation
     * type recorded. Creates the {@link Inventory} row if this is the first stock ever recorded at
     * this sku/location (e.g. adjusting in "found" stock, or a count profit at a previously-empty slot).
     * <p>
     * This method owns exactly one thing: lock the row (via {@link #findOrCreateInventory}) → read the
     * "before" audit snapshot only after that lock is held → apply the atomic guarded UPDATE(s) → read
     * "after" (still within the same lock, so it reflects exactly this call's own change, nothing a
     * concurrent transaction could have interleaved) → write the movement. Every caller gets a
     * before/after pair that is internally consistent (after - before == the change actually applied)
     * even under concurrent callers hammering the same row, because the row lock serializes them.
     * <p>
     * Deliberately does not gate on {@link #assertLocationUsable}: whether the current location status
     * is allowed to receive this change is a business-scenario decision, not something this low-level
     * mutation primitive should hardcode. Stock-count-task's location is expected to be COUNTING at
     * the moment its result gets applied here, and stock-adjust-order's {@code confirm()} does call
     * {@link #assertLocationUsable} itself before invoking this method — see that call site.
     */
    @Transactional
    public StockMovement increaseOnHand(Warehouse warehouse, WarehouseArea area, WarehouseLocation location, Sku sku,
            int qty, MovementType movementType, OperationType operationType, String businessNo, String remark) {
        Inventory inventory = findOrCreateInventory(warehouse, area, location, sku);
        InventorySnapshot before = inventory.snapshot();
        inventoryMapper.increaseOnHand(inventory.getId(), qty);
        if (area.getAreaType() == AreaType.EXCEPTION) {
            inventoryMapper.increaseFrozen(inventory.getId(), qty);
        }
        InventorySnapshot after = requireInventoryById(inventory.getId()).snapshot();

        StockMovement movement = new StockMovement(
                movementType, operationType, warehouse, area, location, sku,
                qty, before, after,
                businessNo, remark
        );
        stockMovementMapper.insert(movement);
        return movement;
    }

    /**
     * Counterpart to {@link #increaseOnHand} — same locking/before-after contract, via
     * {@link #requireInventory} instead of {@link #findOrCreateInventory} (a decrease can never
     * legitimately target a row that doesn't exist yet). The guarded {@link InventoryMapper#decreaseOnHand}
     * update enforces on-hand staying at or above reserved+frozen in one atomic condition, covering
     * "cannot go negative" and "cannot dip below locked/frozen" in a single check. Also does not gate
     * on {@link #assertLocationUsable}; see {@link #increaseOnHand} for why that's the caller's job.
     */
    @Transactional
    public StockMovement decreaseOnHand(Warehouse warehouse, WarehouseArea area, WarehouseLocation location, Sku sku,
            int qty, MovementType movementType, OperationType operationType, String businessNo, String remark) {
        Inventory inventory = requireInventory(warehouse.getId(), sku.getId(), area.getId(), location.getId());
        InventorySnapshot before = inventory.snapshot();
        int affected = inventoryMapper.decreaseOnHand(inventory.getId(), qty);
        if (affected == 0) {
            throw new BusinessException("on-hand quantity at location " + location.getLocationCode() + " is insufficient to decrease by " + qty);
        }
        InventorySnapshot after = new InventorySnapshot(before.onHandQty() - qty, before.lockedQty(), before.frozenQty());

        StockMovement movement = new StockMovement(
                movementType, operationType, warehouse, area, location, sku,
                -qty, before, after,
                businessNo, remark
        );
        stockMovementMapper.insert(movement);
        return movement;
    }

    @Transactional
    public void lockForOutbound(Warehouse warehouse, Sku sku, int qty, Long outboundOrderId, Long outboundOrderItemId, String businessNo) {
        List<Inventory> candidates = loadAllocationCandidates(warehouse.getId(), sku.getId());
        int remaining = qty;

        for (Inventory inventory : candidates) {
            if (remaining <= 0) {
                break;
            }
            int take = Math.min(inventory.getAvailableQuantity(), remaining);
            if (take <= 0) {
                continue;
            }
            int affected = inventoryMapper.tryLock(inventory.getId(), take);
            if (affected == 0) {
                continue;
            }

            InventorySnapshot before = inventory.snapshot();
            InventorySnapshot after = new InventorySnapshot(before.onHandQty(), before.lockedQty() + take, before.frozenQty());
            OutboundStockLock lock = new OutboundStockLock(outboundOrderId, outboundOrderItemId, sku, warehouse, inventory.getArea(), inventory.getLocation(), take);
            outboundStockLockMapper.insert(lock);
            stockMovementMapper.insert(new StockMovement(
                    MovementType.LOCK, OperationType.OUTBOUND_LOCK, warehouse, inventory.getArea(), inventory.getLocation(), sku,
                    0, before, after,
                    businessNo, "lock inventory for outbound order"
            ));
            remaining -= take;
        }

        if (remaining > 0) {
            wmsExceptionEventService.record(
                    ExceptionType.INVENTORY_NOT_ENOUGH,
                    businessNo,
                    sku.getId(),
                    warehouse.getId(),
                    null,
                    null,
                    "available inventory is insufficient for sku " + sku.getCode() + ", short by " + remaining
            );
            throw new BusinessException("available inventory is insufficient for sku " + sku.getCode());
        }
    }

    @Transactional
    public void confirmShip(Long outboundOrderId, String businessNo) {
        List<OutboundStockLock> locks = outboundStockLockMapper.selectList(Wrappers.lambdaQuery(OutboundStockLock.class)
                .eq(OutboundStockLock::getOutboundOrderId, outboundOrderId)
                .eq(OutboundStockLock::getStatus, LockStatus.LOCKED));
        if (locks.isEmpty()) {
            throw new BusinessException("no locked allocation found for this outbound order");
        }

        for (OutboundStockLock lock : locks) {
            Inventory inventory = requireInventory(lock.getWarehouseId(), lock.getSkuId(), lock.getAreaId(), lock.getLocationId());
            InventorySnapshot before = inventory.snapshot();
            int affected = inventoryMapper.confirmShip(inventory.getId(), lock.getLockQty());
            if (affected == 0) {
                throw new BusinessException("confirm ship failed, inventory state changed concurrently");
            }
            InventorySnapshot after = new InventorySnapshot(
                    before.onHandQty() - lock.getLockQty(),
                    before.lockedQty() - lock.getLockQty(),
                    before.frozenQty()
            );

            Sku sku = skuService.getById(lock.getSkuId());
            Warehouse warehouse = warehouseService.getById(lock.getWarehouseId());
            WarehouseArea area = warehouseAreaService.getById(lock.getAreaId());
            WarehouseLocation location = warehouseLocationService.getById(lock.getLocationId());

            stockMovementMapper.insert(new StockMovement(
                    MovementType.OUTBOUND, OperationType.OUTBOUND_SHIP, warehouse, area, location, sku,
                    -lock.getLockQty(), before, after,
                    businessNo, "confirm ship outbound order"
            ));

            warehouseLocationMapper.release(lock.getLocationId(), lock.getLockQty());
            lock.markShipped();
            outboundStockLockMapper.updateById(lock);
        }
    }

    @Transactional
    public void releaseLocks(Long outboundOrderId, String businessNo) {
        List<OutboundStockLock> locks = outboundStockLockMapper.selectList(Wrappers.lambdaQuery(OutboundStockLock.class)
                .eq(OutboundStockLock::getOutboundOrderId, outboundOrderId)
                .eq(OutboundStockLock::getStatus, LockStatus.LOCKED));

        for (OutboundStockLock lock : locks) {
            Inventory inventory = requireInventory(lock.getWarehouseId(), lock.getSkuId(), lock.getAreaId(), lock.getLocationId());
            InventorySnapshot before = inventory.snapshot();
            int affected = inventoryMapper.releaseLock(inventory.getId(), lock.getLockQty());
            if (affected == 0) {
                throw new BusinessException("release lock failed, inventory state changed concurrently");
            }
            InventorySnapshot after = new InventorySnapshot(before.onHandQty(), before.lockedQty() - lock.getLockQty(), before.frozenQty());

            Sku sku = skuService.getById(lock.getSkuId());
            Warehouse warehouse = warehouseService.getById(lock.getWarehouseId());
            WarehouseArea area = warehouseAreaService.getById(lock.getAreaId());
            WarehouseLocation location = warehouseLocationService.getById(lock.getLocationId());

            stockMovementMapper.insert(new StockMovement(
                    MovementType.UNLOCK, OperationType.OUTBOUND_CANCEL_UNLOCK, warehouse, area, location, sku,
                    0, before, after,
                    businessNo, "release lock for cancelled outbound order"
            ));

            lock.markReleased();
            outboundStockLockMapper.updateById(lock);
        }
    }

    private List<Inventory> loadAllocationCandidates(Long warehouseId, Long skuId) {
        List<Inventory> candidates = inventoryMapper.selectList(Wrappers.lambdaQuery(Inventory.class)
                .eq(Inventory::getWarehouseId, warehouseId)
                .eq(Inventory::getSkuId, skuId)
                .eq(Inventory::getInventoryStatus, InventoryStatus.NORMAL)
                .isNotNull(Inventory::getLocationId));

        List<Inventory> usable = new ArrayList<>();
        for (Inventory inventory : candidates) {
            if (inventory.getAvailableQuantity() <= 0) {
                continue;
            }
            WarehouseLocation location = warehouseLocationService.getById(inventory.getLocationId());
            if (!location.isUsable()) {
                continue;
            }
            WarehouseArea area = warehouseAreaService.getById(inventory.getAreaId());
            inventory.attachLocation(location);
            inventory.attachArea(area);
            usable.add(inventory);
        }

        usable.sort(Comparator
                .<Inventory>comparingInt(inv -> inv.getArea().getAreaType() == AreaType.PICKING ? 0 : 1)
                .thenComparingInt(inv -> inv.getLocation().getPickPriority())
                .thenComparing(Inventory::getAvailableQuantity, Comparator.reverseOrder()));

        return usable;
    }

    public void assertLocationUsable(WarehouseLocation location, Sku sku, Warehouse warehouse, WarehouseArea area, String businessNo, String action) {
        if (location.isUsable()) {
            return;
        }
        ExceptionType exceptionType = switch (location.getStatus()) {
            case DISABLED -> ExceptionType.LOCATION_DISABLED;
            case LOCKED -> ExceptionType.LOCATION_LOCKED;
            case COUNTING -> ExceptionType.LOCATION_COUNTING;
            default -> ExceptionType.LOCATION_CAPACITY_NOT_ENOUGH;
        };
        wmsExceptionEventService.record(exceptionType, businessNo, sku.getId(), warehouse.getId(), area.getId(), location.getId(),
                "location " + location.getLocationCode() + " status is " + location.getStatus() + ", cannot " + action);
        throw new BusinessException("location " + location.getLocationCode() + " is not usable for " + action + " (status=" + location.getStatus() + ")");
    }

    /**
     * Locks the target row (or creates and locks it, for the first stock ever recorded at this
     * sku/location) before returning it, so every caller's subsequent "read before snapshot, mutate,
     * read after snapshot" sequence is race-free: no other transaction can touch this row until this
     * one commits or rolls back. Relies on the existing {@code uk_inventory_dimension} unique
     * constraint to make the create path safe too — if two transactions both find no row and both
     * try to insert, the loser gets a DuplicateKeyException instead of a duplicate row, and simply
     * re-locks the row the winner just committed.
     */
    private Inventory findOrCreateInventory(Warehouse warehouse, WarehouseArea area, WarehouseLocation location, Sku sku) {
        Inventory inventory = inventoryMapper.selectByDimensionForUpdate(warehouse.getId(), sku.getId(), area.getId(), location.getId());
        if (inventory != null) {
            return inventory;
        }
        Inventory created = new Inventory(warehouse, area, location, sku);
        try {
            inventoryMapper.insert(created);
            return created;
        } catch (DuplicateKeyException raceLostToConcurrentCreate) {
            Inventory existing = inventoryMapper.selectByDimensionForUpdate(warehouse.getId(), sku.getId(), area.getId(), location.getId());
            if (existing == null) {
                throw new BusinessException("failed to resolve inventory row after a concurrent create conflict");
            }
            return existing;
        }
    }

    private Inventory findInventory(Long warehouseId, Long skuId, Long areaId, Long locationId) {
        return inventoryMapper.selectOne(Wrappers.lambdaQuery(Inventory.class)
                .eq(Inventory::getWarehouseId, warehouseId)
                .eq(Inventory::getSkuId, skuId)
                .eq(Inventory::getAreaId, areaId)
                .eq(Inventory::getLocationId, locationId));
    }

    /** Locked counterpart to {@link #findInventory}; see {@link #findOrCreateInventory} for why. */
    private Inventory requireInventory(Long warehouseId, Long skuId, Long areaId, Long locationId) {
        Inventory inventory = inventoryMapper.selectByDimensionForUpdate(warehouseId, skuId, areaId, locationId);
        if (inventory == null) {
            throw new BusinessException("inventory not found for locked allocation");
        }
        return inventory;
    }

    /**
     * Looks an inventory row up by its own id (as opposed to {@link #findInventory}, which looks it
     * up by dimension). Used by stock-adjust-order to anchor an adjustment to one specific balance
     * row, re-resolved fresh at confirm time rather than trusting anything cached at create time.
     */
    public Inventory requireInventoryById(Long id) {
        Inventory inventory = inventoryMapper.selectById(id);
        if (inventory == null) {
            throw new BusinessException("inventory record not found: " + id);
        }
        return assemble(inventory);
    }

    /** Exposes {@link #findInventory} for stock-adjust-order to look up the row an off-book increase just landed on. */
    public Inventory getByDimension(Long warehouseId, Long skuId, Long areaId, Long locationId) {
        return findInventory(warehouseId, skuId, areaId, locationId);
    }

    private Inventory assemble(Inventory inventory) {
        inventory.attachWarehouse(warehouseService.getById(inventory.getWarehouseId()));
        inventory.attachSku(skuService.getById(inventory.getSkuId()));
        if (inventory.getAreaId() != null) {
            inventory.attachArea(warehouseAreaService.getById(inventory.getAreaId()));
        }
        if (inventory.getLocationId() != null) {
            inventory.attachLocation(warehouseLocationService.getById(inventory.getLocationId()));
        }
        return inventory;
    }

    private StockMovement assembleMovement(StockMovement movement) {
        movement.attachWarehouse(warehouseService.getById(movement.getWarehouseId()));
        movement.attachSku(skuService.getById(movement.getSkuId()));
        if (movement.getAreaId() != null) {
            movement.attachArea(warehouseAreaService.getById(movement.getAreaId()));
        }
        if (movement.getLocationId() != null) {
            movement.attachLocation(warehouseLocationService.getById(movement.getLocationId()));
        }
        return movement;
    }
}
