package com.example.wms.admin.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.wms.admin.model.entity.Inventory;
import com.example.wms.admin.model.entity.Sku;
import com.example.wms.admin.model.entity.StockCountItem;
import com.example.wms.admin.model.entity.StockCountTask;
import com.example.wms.admin.model.entity.Warehouse;
import com.example.wms.admin.model.entity.WarehouseArea;
import com.example.wms.admin.model.entity.WarehouseLocation;
import com.example.wms.admin.model.mapper.InventoryMapper;
import com.example.wms.admin.model.mapper.StockCountItemMapper;
import com.example.wms.admin.model.mapper.StockCountTaskMapper;
import com.example.wms.admin.security.CurrentUser;
import com.example.wms.admin.security.CurrentUserContext;
import com.example.wms.admin.view.dto.CreateStockCountTaskRequest;
import com.example.wms.admin.view.dto.StockCountItemActualRequest;
import com.example.wms.admin.view.dto.StockCountTaskQuery;
import com.example.wms.admin.view.dto.StockCountTaskResponse;
import com.example.wms.admin.view.dto.UpdateStatusRequest;
import com.example.wms.admin.view.dto.UpdateStockCountItemsRequest;
import com.example.wms.common.common.BusinessException;
import com.example.wms.common.common.PageResponse;
import com.example.wms.common.enums.BizNoType;
import com.example.wms.common.enums.LocationStatus;
import com.example.wms.common.enums.MovementType;
import com.example.wms.common.enums.OperationType;
import com.example.wms.common.enums.StockCountItemStatus;
import com.example.wms.common.enums.StockCountTaskStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class StockCountTaskService {

    private final StockCountTaskMapper stockCountTaskMapper;
    private final StockCountItemMapper stockCountItemMapper;
    private final InventoryMapper inventoryMapper;
    private final WarehouseService warehouseService;
    private final SkuService skuService;
    private final WarehouseAreaService warehouseAreaService;
    private final WarehouseLocationService warehouseLocationService;
    private final InventoryService inventoryService;
    private final BizNoGeneratorService bizNoGeneratorService;

    public StockCountTaskService(
            StockCountTaskMapper stockCountTaskMapper,
            StockCountItemMapper stockCountItemMapper,
            InventoryMapper inventoryMapper,
            WarehouseService warehouseService,
            SkuService skuService,
            WarehouseAreaService warehouseAreaService,
            WarehouseLocationService warehouseLocationService,
            InventoryService inventoryService,
            BizNoGeneratorService bizNoGeneratorService
    ) {
        this.stockCountTaskMapper = stockCountTaskMapper;
        this.stockCountItemMapper = stockCountItemMapper;
        this.inventoryMapper = inventoryMapper;
        this.warehouseService = warehouseService;
        this.skuService = skuService;
        this.warehouseAreaService = warehouseAreaService;
        this.warehouseLocationService = warehouseLocationService;
        this.inventoryService = inventoryService;
        this.bizNoGeneratorService = bizNoGeneratorService;
    }

    /** The system always mints its own count number; any {@code countNo} the client sends is ignored. */
    @Transactional
    public StockCountTaskResponse create(CreateStockCountTaskRequest request) {
        Warehouse warehouse = warehouseService.getById(request.warehouseId());
        WarehouseArea area = null;
        if (request.areaId() != null) {
            area = warehouseAreaService.getById(request.areaId());
            if (!area.getWarehouseId().equals(warehouse.getId())) {
                throw new BusinessException("area does not belong to the given warehouse");
            }
        }
        WarehouseLocation location = null;
        if (request.locationId() != null) {
            location = warehouseLocationService.getById(request.locationId());
            if (area != null && !location.getAreaId().equals(area.getId())) {
                throw new BusinessException("location does not belong to the given area");
            }
        }
        StockCountTask task = new StockCountTask(bizNoGeneratorService.generate(BizNoType.STOCK_COUNT_TASK), warehouse, area, location, request.remark(), currentUsername());
        stockCountTaskMapper.insert(task);
        return getDetail(task.getId());
    }

    @Transactional
    public StockCountTaskResponse start(Long id) {
        StockCountTask task = assemble(getById(id));
        if (task.getStatus() != StockCountTaskStatus.DRAFT) {
            throw new BusinessException("stock count task can only be started from draft");
        }

        List<Inventory> scoped = loadScopedInventory(task);
        Map<Long, WarehouseLocation> locationsById = new LinkedHashMap<>();
        for (Inventory inventory : scoped) {
            WarehouseLocation location = locationsById.computeIfAbsent(inventory.getLocationId(), warehouseLocationService::getById);
            if (location.getStatus() != LocationStatus.ENABLED) {
                throw new BusinessException("location " + location.getLocationCode() + " is not enabled, cannot start counting (status=" + location.getStatus() + ")");
            }
        }

        for (Inventory inventory : scoped) {
            Sku sku = skuService.getById(inventory.getSkuId());
            WarehouseArea area = warehouseAreaService.getById(inventory.getAreaId());
            WarehouseLocation location = locationsById.get(inventory.getLocationId());
            stockCountItemMapper.insert(new StockCountItem(task, sku, area, location, inventory.snapshot()));
        }

        for (Long locationId : locationsById.keySet()) {
            warehouseLocationService.updateStatus(locationId, new UpdateStatusRequest(LocationStatus.COUNTING.name()));
        }

        task.markCounting();
        stockCountTaskMapper.updateById(task);
        return getDetail(id);
    }

    @Transactional
    public StockCountTaskResponse record(Long id, UpdateStockCountItemsRequest request) {
        StockCountTask task = getById(id);
        if (task.getStatus() != StockCountTaskStatus.COUNTING) {
            throw new BusinessException("stock count task must be counting before recording actual quantities");
        }
        for (StockCountItemActualRequest actual : request.items()) {
            StockCountItem item = stockCountItemMapper.selectById(actual.itemId());
            if (item == null || !item.getCountTaskId().equals(id)) {
                throw new BusinessException("count item not found for this task: " + actual.itemId());
            }
            item.recordActual(actual.actualQty(), actual.remark());
            stockCountItemMapper.updateById(item);
        }
        return getDetail(id);
    }

    /**
     * Deliberately does not call {@link InventoryService#assertLocationUsable} before applying
     * profit/loss: every location involved is expected to be COUNTING right now (this task itself put
     * them there in {@link #start}), and finishing the count is exactly the operation that's allowed
     * to write to a COUNTING location — a plain stock adjustment is not (see
     * {@link StockAdjustOrderService#confirm}, which does call that guard). Row-level locking and
     * before/after snapshot correctness under concurrent completions is handled inside
     * {@link InventoryService#increaseOnHand}/{@code decreaseOnHand} themselves.
     */
    @Transactional
    public StockCountTaskResponse complete(Long id) {
        StockCountTask task = assemble(getById(id));
        if (task.getStatus() != StockCountTaskStatus.COUNTING) {
            throw new BusinessException("stock count task must be counting before it can be completed");
        }

        List<StockCountItem> items = task.getItems();
        boolean allRecorded = items.stream().allMatch(item -> item.getStatus() == StockCountItemStatus.RECORDED);
        if (!allRecorded) {
            throw new BusinessException("not all count items have recorded an actual quantity yet");
        }

        for (StockCountItem item : items) {
            int diff = item.getDiffQty();
            if (diff > 0) {
                inventoryService.increaseOnHand(task.getWarehouse(), item.getArea(), item.getLocation(), item.getSku(),
                        diff, MovementType.COUNT, OperationType.STOCK_COUNT_PROFIT, task.getCountNo(), "stock count task profit");
            } else if (diff < 0) {
                inventoryService.decreaseOnHand(task.getWarehouse(), item.getArea(), item.getLocation(), item.getSku(),
                        -diff, MovementType.COUNT, OperationType.STOCK_COUNT_LOSS, task.getCountNo(), "stock count task loss");
            }
        }

        restoreLocations(items);

        task.markCompleted(currentUsername());
        stockCountTaskMapper.updateById(task);
        return StockCountTaskResponse.from(task);
    }

    @Transactional
    public StockCountTaskResponse cancel(Long id, String reason) {
        StockCountTask task = getById(id);
        if (task.getStatus() != StockCountTaskStatus.DRAFT && task.getStatus() != StockCountTaskStatus.COUNTING) {
            throw new BusinessException("stock count task cannot be cancelled in its current state");
        }
        if (task.getStatus() == StockCountTaskStatus.COUNTING) {
            List<StockCountItem> items = stockCountItemMapper.selectList(Wrappers.lambdaQuery(StockCountItem.class)
                    .eq(StockCountItem::getCountTaskId, id));
            restoreLocations(items);
        }
        task.markCancelled(currentUsername(), reason);
        stockCountTaskMapper.updateById(task);
        return StockCountTaskResponse.from(assemble(task));
    }

    @Transactional(readOnly = true)
    public StockCountTaskResponse getDetail(Long id) {
        return StockCountTaskResponse.from(assemble(getById(id)));
    }

    @Transactional(readOnly = true)
    public PageResponse<StockCountTaskResponse> search(StockCountTaskQuery query) {
        Page<StockCountTask> page = stockCountTaskMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                Wrappers.lambdaQuery(StockCountTask.class)
                        .like(StringUtils.hasText(query.getCountNo()), StockCountTask::getCountNo, query.getCountNo())
                        .eq(query.getWarehouseId() != null, StockCountTask::getWarehouseId, query.getWarehouseId())
                        .eq(query.getAreaId() != null, StockCountTask::getAreaId, query.getAreaId())
                        .eq(query.getLocationId() != null, StockCountTask::getLocationId, query.getLocationId())
                        .eq(query.getStatus() != null, StockCountTask::getStatus, query.getStatus())
                        .ge(query.getStartTime() != null, StockCountTask::getCreatedAt, query.getStartTime())
                        .le(query.getEndTime() != null, StockCountTask::getCreatedAt, query.getEndTime())
                        .orderByDesc(StockCountTask::getCreatedAt)
        );

        return PageResponse.from(page, task -> StockCountTaskResponse.from(assemble(task)));
    }

    private List<Inventory> loadScopedInventory(StockCountTask task) {
        return inventoryMapper.selectList(Wrappers.lambdaQuery(Inventory.class)
                .eq(Inventory::getWarehouseId, task.getWarehouseId())
                .eq(task.getAreaId() != null, Inventory::getAreaId, task.getAreaId())
                .eq(task.getLocationId() != null, Inventory::getLocationId, task.getLocationId())
                .isNotNull(Inventory::getLocationId));
    }

    private void restoreLocations(List<StockCountItem> items) {
        items.stream().map(StockCountItem::getLocationId).distinct()
                .forEach(locationId -> warehouseLocationService.updateStatus(locationId, new UpdateStatusRequest(LocationStatus.ENABLED.name())));
    }

    private StockCountTask getById(Long id) {
        StockCountTask task = stockCountTaskMapper.selectById(id);
        if (task == null) {
            throw new BusinessException("stock count task not found");
        }
        return task;
    }

    private StockCountTask assemble(StockCountTask task) {
        task.attachWarehouse(warehouseService.getById(task.getWarehouseId()));
        if (task.getAreaId() != null) {
            task.attachArea(warehouseAreaService.getById(task.getAreaId()));
        }
        if (task.getLocationId() != null) {
            task.attachLocation(warehouseLocationService.getById(task.getLocationId()));
        }
        List<StockCountItem> items = stockCountItemMapper.selectList(Wrappers.lambdaQuery(StockCountItem.class)
                .eq(StockCountItem::getCountTaskId, task.getId())
                .orderByAsc(StockCountItem::getId));
        items.forEach(item -> {
            item.attachSku(skuService.getById(item.getSkuId()));
            item.attachArea(warehouseAreaService.getById(item.getAreaId()));
            item.attachLocation(warehouseLocationService.getById(item.getLocationId()));
        });
        task.setItems(items);
        return task;
    }

    private String currentUsername() {
        CurrentUser currentUser = CurrentUserContext.get();
        return currentUser != null ? currentUser.username() : "system";
    }
}
