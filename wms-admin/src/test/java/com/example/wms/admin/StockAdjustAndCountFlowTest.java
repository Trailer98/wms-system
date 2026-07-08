package com.example.wms.admin;

import com.example.wms.admin.service.InboundOrderService;
import com.example.wms.admin.service.InventoryService;
import com.example.wms.admin.service.SkuService;
import com.example.wms.admin.service.StockAdjustOrderService;
import com.example.wms.admin.service.StockCountTaskService;
import com.example.wms.admin.service.WarehouseAreaService;
import com.example.wms.admin.service.WarehouseLocationService;
import com.example.wms.admin.service.WarehouseService;
import com.example.wms.admin.view.dto.CreateInboundOrderRequest;
import com.example.wms.admin.view.dto.CreateSkuRequest;
import com.example.wms.admin.view.dto.CreateStockAdjustOrderRequest;
import com.example.wms.admin.view.dto.CreateStockCountTaskRequest;
import com.example.wms.admin.view.dto.CreateWarehouseAreaRequest;
import com.example.wms.admin.view.dto.CreateWarehouseLocationRequest;
import com.example.wms.admin.view.dto.CreateWarehouseRequest;
import com.example.wms.admin.view.dto.InboundOrderItemRequest;
import com.example.wms.admin.view.dto.InventoryQuery;
import com.example.wms.admin.view.dto.InventoryResponse;
import com.example.wms.admin.view.dto.SkuResponse;
import com.example.wms.admin.view.dto.StockAdjustOrderItemRequest;
import com.example.wms.admin.view.dto.StockAdjustOrderResponse;
import com.example.wms.admin.view.dto.StockCountItemActualRequest;
import com.example.wms.admin.view.dto.StockCountTaskResponse;
import com.example.wms.admin.view.dto.StockMovementQuery;
import com.example.wms.admin.view.dto.StockMovementResponse;
import com.example.wms.admin.view.dto.UpdateStatusRequest;
import com.example.wms.admin.view.dto.UpdateStockCountItemsRequest;
import com.example.wms.admin.view.dto.WarehouseAreaResponse;
import com.example.wms.admin.view.dto.WarehouseLocationResponse;
import com.example.wms.admin.view.dto.WarehouseResponse;
import com.example.wms.common.common.BusinessException;
import com.example.wms.common.common.PageResponse;
import com.example.wms.common.enums.AdjustAction;
import com.example.wms.common.enums.AdjustReasonType;
import com.example.wms.common.enums.AreaType;
import com.example.wms.common.enums.AdjustType;
import com.example.wms.common.enums.HoldStatus;
import com.example.wms.common.enums.LocationStatus;
import com.example.wms.common.enums.LocationType;
import com.example.wms.common.enums.MovementType;
import com.example.wms.common.enums.OperationType;
import com.example.wms.common.enums.StockAdjustOrderStatus;
import com.example.wms.common.enums.StockCountTaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = "spring.datasource.url=jdbc:mysql://localhost:3306/wms_system_test?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true")
class StockAdjustAndCountFlowTest {

    @Autowired
    private WarehouseService warehouseService;
    @Autowired
    private WarehouseAreaService warehouseAreaService;
    @Autowired
    private WarehouseLocationService warehouseLocationService;
    @Autowired
    private SkuService skuService;
    @Autowired
    private InboundOrderService inboundOrderService;
    @Autowired
    private InventoryService inventoryService;
    @Autowired
    private StockAdjustOrderService stockAdjustOrderService;
    @Autowired
    private StockCountTaskService stockCountTaskService;

    private String suffix;
    private Long warehouseId;
    private Long areaId;
    private Long locationId;
    private Long exceptionAreaId;
    private Long exceptionLocationId;
    private Long skuId;

    @BeforeEach
    void setUp() {
        suffix = String.valueOf(System.nanoTime());

        WarehouseResponse warehouse = warehouseService.create(new CreateWarehouseRequest("WH-" + suffix, "测试仓" + suffix, "test address"));
        warehouseId = warehouse.id();

        SkuResponse sku = skuService.create(new CreateSkuRequest("SKU-" + suffix, "测试SKU" + suffix, "EA", "TEST"));
        skuId = sku.id();

        WarehouseAreaResponse area = warehouseAreaService.create(new CreateWarehouseAreaRequest(warehouseId, "STO-" + suffix, "存储区", AreaType.STORAGE, 10, null));
        areaId = area.id();

        WarehouseLocationResponse location = warehouseLocationService.create(new CreateWarehouseLocationRequest(warehouseId, areaId, "A-01-" + suffix, "A-01", LocationType.NORMAL, 1000, true, 1, null));
        locationId = location.id();

        WarehouseAreaResponse exceptionArea = warehouseAreaService.create(new CreateWarehouseAreaRequest(warehouseId, "EXC-" + suffix, "异常区", AreaType.EXCEPTION, 10, null));
        exceptionAreaId = exceptionArea.id();

        WarehouseLocationResponse exceptionLocation = warehouseLocationService.create(new CreateWarehouseLocationRequest(warehouseId, exceptionAreaId, "E-01-" + suffix, "E-01", LocationType.NORMAL, 1000, true, 1, null));
        exceptionLocationId = exceptionLocation.id();
    }

    private void receiveInto(int quantity) {
        CreateInboundOrderRequest request = new CreateInboundOrderRequest(
                "IN-" + suffix + "-" + System.nanoTime(), warehouseId, null,
                List.of(new InboundOrderItemRequest(skuId, quantity, areaId, locationId))
        );
        var order = inboundOrderService.create(request);
        inboundOrderService.receive(order.id());
    }

    private InventoryResponse fetchInventory() {
        InventoryQuery query = new InventoryQuery();
        query.setLocationId(locationId);
        PageResponse<InventoryResponse> page = inventoryService.search(query);
        assertEquals(1, page.records().size());
        return page.records().get(0);
    }

    private StockMovementResponse findMovement(String businessNo, OperationType operationType) {
        StockMovementQuery query = new StockMovementQuery();
        query.setBusinessNo(businessNo);
        PageResponse<StockMovementResponse> movements = inventoryService.searchTransactions(query);
        return movements.records().stream()
                .filter(m -> m.operationType() == operationType)
                .findFirst()
                .orElseThrow(() -> new AssertionError("no " + operationType + " movement found for " + businessNo));
    }

    private StockAdjustOrderResponse createAdjustOrderFromInventory(Long inventoryId, AdjustType adjustType, int qty, String remark) {
        String adjustNo = "ADJ-" + suffix + "-" + System.nanoTime();
        CreateStockAdjustOrderRequest request = new CreateStockAdjustOrderRequest(
                adjustNo, warehouseId, AdjustReasonType.DATA_ERROR, "test adjust",
                List.of(new StockAdjustOrderItemRequest(inventoryId, null, null, null, adjustType, qty, false, remark))
        );
        return stockAdjustOrderService.create(request);
    }

    private StockAdjustOrderResponse createOffBookAdjustOrder(AdjustType adjustType, int qty, boolean allowCreateInventory, String remark) {
        String adjustNo = "ADJ-" + suffix + "-" + System.nanoTime();
        CreateStockAdjustOrderRequest request = new CreateStockAdjustOrderRequest(
                adjustNo, warehouseId, AdjustReasonType.FOUND, "off-book test",
                List.of(new StockAdjustOrderItemRequest(null, skuId, areaId, locationId, adjustType, qty, allowCreateInventory, remark))
        );
        return stockAdjustOrderService.create(request);
    }

    private InventoryResponse fetchInventoryAt(Long locationId) {
        InventoryQuery query = new InventoryQuery();
        query.setLocationId(locationId);
        PageResponse<InventoryResponse> page = inventoryService.search(query);
        assertEquals(1, page.records().size());
        return page.records().get(0);
    }

    private StockAdjustOrderResponse createTransferAdjustOrder(Long inventoryId, AdjustAction action, Long targetAreaId, Long targetLocationId,
            int qty, AdjustReasonType reasonType, String remark) {
        String adjustNo = "ADJ-" + suffix + "-" + System.nanoTime();
        CreateStockAdjustOrderRequest request = new CreateStockAdjustOrderRequest(
                adjustNo, warehouseId, reasonType, "transfer test",
                List.of(new StockAdjustOrderItemRequest(inventoryId, null, null, null, AdjustType.DECREASE, action, qty, false,
                        warehouseId, targetAreaId, targetLocationId, remark))
        );
        return stockAdjustOrderService.create(request);
    }

    @Test
    void transferToExceptionSucceedsAndKeepsTotalOnHand() {
        receiveInto(100);
        Long sourceInventoryId = fetchInventory().id();

        StockAdjustOrderResponse created = createTransferAdjustOrder(sourceInventoryId, AdjustAction.TRANSFER_TO_EXCEPTION,
                exceptionAreaId, exceptionLocationId, 30, AdjustReasonType.DAMAGE, "damaged in handling");
        assertEquals(AdjustAction.TRANSFER_TO_EXCEPTION, created.items().get(0).adjustAction());
        stockAdjustOrderService.submit(created.id());
        StockAdjustOrderResponse confirmed = stockAdjustOrderService.confirm(created.id());

        assertEquals(StockAdjustOrderStatus.COMPLETED, confirmed.status());
        assertEquals(HoldStatus.CONSUMED, confirmed.items().get(0).holdStatus());

        InventoryResponse sourceAfter = fetchInventory();
        InventoryResponse targetAfter = fetchInventoryAt(exceptionLocationId);

        assertEquals(70, sourceAfter.quantity());
        assertEquals(0, sourceAfter.frozenQuantity());
        assertEquals(70, sourceAfter.availableQuantity());

        assertEquals(30, targetAfter.quantity());
        assertEquals(30, targetAfter.frozenQuantity());
        assertEquals(0, targetAfter.availableQuantity());

        assertEquals(100, sourceAfter.quantity() + targetAfter.quantity(), "total on-hand across both rows must be unchanged");
        assertEquals(sourceInventoryId, confirmed.items().get(0).inventoryId());
        assertEquals(targetAfter.id(), confirmed.items().get(0).targetInventoryId());

        StockMovementResponse out = findMovement(confirmed.adjustNo(), OperationType.TRANSFER_TO_EXCEPTION_OUT);
        assertEquals(100, out.beforeQuantity());
        assertEquals(70, out.afterQuantity());
        StockMovementResponse in = findMovement(confirmed.adjustNo(), OperationType.TRANSFER_TO_EXCEPTION_IN);
        assertEquals(0, in.beforeQuantity());
        assertEquals(30, in.afterQuantity());
    }

    @Test
    void restoreFromExceptionSucceedsAndKeepsTotalOnHand() {
        receiveInto(100);
        Long sourceInventoryId = fetchInventory().id();
        StockAdjustOrderResponse transfer = createTransferAdjustOrder(sourceInventoryId, AdjustAction.TRANSFER_TO_EXCEPTION,
                exceptionAreaId, exceptionLocationId, 40, AdjustReasonType.QUALITY_ISSUE, "failed QC");
        stockAdjustOrderService.submit(transfer.id());
        stockAdjustOrderService.confirm(transfer.id());

        Long exceptionInventoryId = fetchInventoryAt(exceptionLocationId).id();
        StockAdjustOrderResponse restore = createTransferAdjustOrder(exceptionInventoryId, AdjustAction.RESTORE_FROM_EXCEPTION,
                areaId, locationId, 25, AdjustReasonType.OTHER, "rework passed recheck");
        assertEquals(AdjustAction.RESTORE_FROM_EXCEPTION, restore.items().get(0).adjustAction());
        stockAdjustOrderService.submit(restore.id());
        StockAdjustOrderResponse confirmed = stockAdjustOrderService.confirm(restore.id());

        assertEquals(StockAdjustOrderStatus.COMPLETED, confirmed.status());

        InventoryResponse exceptionAfter = fetchInventoryAt(exceptionLocationId);
        InventoryResponse normalAfter = fetchInventory();

        assertEquals(15, exceptionAfter.quantity());
        assertEquals(15, exceptionAfter.frozenQuantity());
        assertEquals(0, exceptionAfter.availableQuantity());

        assertEquals(85, normalAfter.quantity());
        assertEquals(0, normalAfter.frozenQuantity());
        assertEquals(85, normalAfter.availableQuantity());

        assertEquals(100, exceptionAfter.quantity() + normalAfter.quantity(), "total on-hand across both rows must be unchanged");

        StockMovementResponse out = findMovement(confirmed.adjustNo(), OperationType.RESTORE_FROM_EXCEPTION_OUT);
        assertEquals(40, out.beforeQuantity());
        assertEquals(15, out.afterQuantity());
        StockMovementResponse in = findMovement(confirmed.adjustNo(), OperationType.RESTORE_FROM_EXCEPTION_IN);
        assertEquals(60, in.beforeQuantity());
        assertEquals(85, in.afterQuantity());
    }

    @Test
    void damageReasonRejectsPlainQuantityDecrease() {
        receiveInto(50);
        Long inventoryId = fetchInventory().id();
        String adjustNo = "ADJ-" + suffix + "-" + System.nanoTime();
        CreateStockAdjustOrderRequest request = new CreateStockAdjustOrderRequest(
                adjustNo, warehouseId, AdjustReasonType.DAMAGE, "damaged stock",
                List.of(new StockAdjustOrderItemRequest(inventoryId, null, null, null, AdjustType.DECREASE, AdjustAction.QUANTITY_DECREASE, 10, false,
                        null, null, null, "should be rejected"))
        );
        assertThrows(BusinessException.class, () -> stockAdjustOrderService.create(request));
        assertEquals(50, fetchInventory().quantity());
    }

    @Test
    void qualityIssueReasonRejectsPlainQuantityDecreaseEvenWithoutExplicitAction() {
        receiveInto(50);
        Long inventoryId = fetchInventory().id();
        assertThrows(BusinessException.class, () -> createAdjustOrderFromInventoryWithReason(inventoryId, AdjustReasonType.QUALITY_ISSUE, 10));
        assertEquals(50, fetchInventory().quantity());
    }

    private StockAdjustOrderResponse createAdjustOrderFromInventoryWithReason(Long inventoryId, AdjustReasonType reasonType, int qty) {
        String adjustNo = "ADJ-" + suffix + "-" + System.nanoTime();
        CreateStockAdjustOrderRequest request = new CreateStockAdjustOrderRequest(
                adjustNo, warehouseId, reasonType, "reason mapping test",
                List.of(new StockAdjustOrderItemRequest(inventoryId, null, null, null, AdjustType.DECREASE, qty, false, "remark"))
        );
        return stockAdjustOrderService.create(request);
    }

    @Test
    void transferToExceptionTargetNotExceptionAreaFails() {
        receiveInto(50);
        Long inventoryId = fetchInventory().id();
        assertThrows(BusinessException.class, () -> createTransferAdjustOrder(inventoryId, AdjustAction.TRANSFER_TO_EXCEPTION,
                areaId, locationId, 10, AdjustReasonType.DAMAGE, "wrong target area type"));
        assertEquals(50, fetchInventory().quantity());
    }

    @Test
    void restoreFromExceptionTargetStillExceptionFails() {
        receiveInto(50);
        Long sourceInventoryId = fetchInventory().id();
        StockAdjustOrderResponse transfer = createTransferAdjustOrder(sourceInventoryId, AdjustAction.TRANSFER_TO_EXCEPTION,
                exceptionAreaId, exceptionLocationId, 20, AdjustReasonType.DAMAGE, "into exception");
        stockAdjustOrderService.submit(transfer.id());
        stockAdjustOrderService.confirm(transfer.id());

        Long exceptionInventoryId = fetchInventoryAt(exceptionLocationId).id();
        assertThrows(BusinessException.class, () -> createTransferAdjustOrder(exceptionInventoryId, AdjustAction.RESTORE_FROM_EXCEPTION,
                exceptionAreaId, exceptionLocationId, 5, AdjustReasonType.OTHER, "target still exception"));
    }

    @Test
    void transferToExceptionQuantityExceedsAvailableFails() {
        receiveInto(20);
        Long inventoryId = fetchInventory().id();
        assertThrows(BusinessException.class, () -> createTransferAdjustOrder(inventoryId, AdjustAction.TRANSFER_TO_EXCEPTION,
                exceptionAreaId, exceptionLocationId, 50, AdjustReasonType.DAMAGE, "over-transfer"));
        assertEquals(20, fetchInventory().quantity());
    }

    @Test
    void restoreFromExceptionQuantityExceedsOnHandFails() {
        receiveInto(50);
        Long sourceInventoryId = fetchInventory().id();
        StockAdjustOrderResponse transfer = createTransferAdjustOrder(sourceInventoryId, AdjustAction.TRANSFER_TO_EXCEPTION,
                exceptionAreaId, exceptionLocationId, 20, AdjustReasonType.DAMAGE, "into exception");
        stockAdjustOrderService.submit(transfer.id());
        stockAdjustOrderService.confirm(transfer.id());

        Long exceptionInventoryId = fetchInventoryAt(exceptionLocationId).id();
        assertThrows(BusinessException.class, () -> createTransferAdjustOrder(exceptionInventoryId, AdjustAction.RESTORE_FROM_EXCEPTION,
                areaId, locationId, 50, AdjustReasonType.OTHER, "over-restore"));
        assertEquals(20, fetchInventoryAt(exceptionLocationId).quantity());
    }

    @Test
    void transferToExceptionTargetLocationCountingFails() {
        receiveInto(50);
        Long inventoryId = fetchInventory().id();
        warehouseLocationService.updateStatus(exceptionLocationId, new UpdateStatusRequest(LocationStatus.COUNTING.name()));
        assertThrows(BusinessException.class, () -> createTransferAdjustOrder(inventoryId, AdjustAction.TRANSFER_TO_EXCEPTION,
                exceptionAreaId, exceptionLocationId, 10, AdjustReasonType.DAMAGE, "target counting"));
        assertEquals(50, fetchInventory().quantity());
    }

    @Test
    void cancelSubmittedTransferReleasesHoldWithoutMovementOrInventoryChange() {
        receiveInto(100);
        Long inventoryId = fetchInventory().id();
        StockAdjustOrderResponse created = createTransferAdjustOrder(inventoryId, AdjustAction.TRANSFER_TO_EXCEPTION,
                exceptionAreaId, exceptionLocationId, 30, AdjustReasonType.DAMAGE, "will be cancelled");
        stockAdjustOrderService.submit(created.id());

        InventoryResponse afterSubmit = fetchInventory();
        assertEquals(100, afterSubmit.quantity());
        assertEquals(30, afterSubmit.frozenQuantity());
        assertEquals(70, afterSubmit.availableQuantity());

        StockAdjustOrderResponse cancelled = stockAdjustOrderService.cancel(created.id(), "changed my mind");
        assertEquals(StockAdjustOrderStatus.CANCELLED, cancelled.status());
        assertEquals(HoldStatus.RELEASED, cancelled.items().get(0).holdStatus());

        InventoryResponse afterCancel = fetchInventory();
        assertEquals(100, afterCancel.quantity());
        assertEquals(0, afterCancel.frozenQuantity());
        assertEquals(100, afterCancel.availableQuantity());

        StockMovementQuery query = new StockMovementQuery();
        query.setBusinessNo(created.adjustNo());
        PageResponse<StockMovementResponse> movements = inventoryService.searchTransactions(query);
        assertTrue(movements.records().stream().noneMatch(m -> m.operationType() == OperationType.TRANSFER_TO_EXCEPTION_OUT
                || m.operationType() == OperationType.TRANSFER_TO_EXCEPTION_IN), "cancelling before confirm must never write a transfer movement");
        assertTrue(movements.records().stream().anyMatch(m -> m.operationType() == OperationType.STOCK_FREEZE));
        assertTrue(movements.records().stream().anyMatch(m -> m.operationType() == OperationType.STOCK_UNFREEZE));
    }

    @Test
    void adjustIncreaseFromInventoryCompletesAndWritesMovement() {
        receiveInto(50);
        Long inventoryId = fetchInventory().id();
        StockAdjustOrderResponse created = createAdjustOrderFromInventory(inventoryId, AdjustType.INCREASE, 10, "found stock");
        stockAdjustOrderService.submit(created.id());
        StockAdjustOrderResponse confirmed = stockAdjustOrderService.confirm(created.id());

        assertEquals(StockAdjustOrderStatus.COMPLETED, confirmed.status());
        assertEquals(inventoryId, confirmed.items().get(0).inventoryId());
        assertEquals(60, fetchInventory().quantity());

        StockMovementResponse movement = findMovement(confirmed.adjustNo(), OperationType.STOCK_ADJUST_INCREASE);
        assertEquals(MovementType.ADJUSTMENT, movement.type());
        assertEquals(50, movement.beforeQuantity());
        assertEquals(60, movement.afterQuantity());
    }

    @Test
    void adjustDecreaseFromInventoryCompletesAndWritesMovement() {
        receiveInto(100);
        Long inventoryId = fetchInventory().id();
        StockAdjustOrderResponse created = createAdjustOrderFromInventory(inventoryId, AdjustType.DECREASE, 30, "damaged");
        stockAdjustOrderService.submit(created.id());
        StockAdjustOrderResponse confirmed = stockAdjustOrderService.confirm(created.id());

        assertEquals(StockAdjustOrderStatus.COMPLETED, confirmed.status());
        assertEquals(70, fetchInventory().quantity());

        StockMovementResponse movement = findMovement(confirmed.adjustNo(), OperationType.STOCK_ADJUST_DECREASE);
        assertEquals(100, movement.beforeQuantity());
        assertEquals(70, movement.afterQuantity());
    }

    @Test
    void adjustDecreaseBeyondAvailableFailsWithoutPartialChange() {
        receiveInto(20);
        Long inventoryId = fetchInventory().id();
        StockAdjustOrderResponse created = createAdjustOrderFromInventory(inventoryId, AdjustType.DECREASE, 50, "over-decrease");
        stockAdjustOrderService.submit(created.id());

        assertThrows(BusinessException.class, () -> stockAdjustOrderService.confirm(created.id()));

        assertEquals(20, fetchInventory().quantity());
        StockMovementQuery query = new StockMovementQuery();
        query.setBusinessNo(created.adjustNo());
        PageResponse<StockMovementResponse> movements = inventoryService.searchTransactions(query);
        assertTrue(movements.records().isEmpty(), "no movement should be written when adjust confirmation fails");
    }

    @Test
    void decreaseWithoutInventoryIdIsRejectedAtCreation() {
        assertThrows(BusinessException.class, () -> createOffBookAdjustOrder(AdjustType.DECREASE, 10, true, "cannot decrease off-book"));
    }

    @Test
    void offBookIncreaseWithoutAllowCreateFlagIsRejectedAtCreation() {
        assertThrows(BusinessException.class, () -> createOffBookAdjustOrder(AdjustType.INCREASE, 10, false, "missing flag"));
    }

    @Test
    void offBookIncreaseCreatesInventoryAndWritesMovement() {
        // Deliberately no receiveInto(): this location has no inventory row yet, mirroring "found
        // stock the system never recorded".
        StockAdjustOrderResponse created = createOffBookAdjustOrder(AdjustType.INCREASE, 15, true, "found on floor");
        assertTrue(created.items().get(0).offBookIncrease());

        stockAdjustOrderService.submit(created.id());
        StockAdjustOrderResponse confirmed = stockAdjustOrderService.confirm(created.id());

        assertEquals(StockAdjustOrderStatus.COMPLETED, confirmed.status());
        assertEquals(15, fetchInventory().quantity());
        assertEquals(skuId, confirmed.items().get(0).skuId());
        assertNotNull(confirmed.items().get(0).inventoryId(), "off-book item should be linked to the inventory row it just created");

        StockMovementResponse movement = findMovement(confirmed.adjustNo(), OperationType.STOCK_ADJUST_INCREASE);
        assertEquals(0, movement.beforeQuantity());
        assertEquals(15, movement.afterQuantity());
    }

    @Test
    void inventoryIdTakesPrecedenceOverMismatchedSkuId() {
        receiveInto(40);
        Long inventoryId = fetchInventory().id();
        SkuResponse otherSku = skuService.create(new CreateSkuRequest("SKU-OTHER-" + suffix, "另一个SKU" + suffix, "EA", "TEST"));

        String adjustNo = "ADJ-" + suffix + "-" + System.nanoTime();
        CreateStockAdjustOrderRequest request = new CreateStockAdjustOrderRequest(
                adjustNo, warehouseId, AdjustReasonType.DATA_ERROR, "mismatched sku test",
                // skuId here is deliberately wrong; inventoryId must win.
                List.of(new StockAdjustOrderItemRequest(inventoryId, otherSku.id(), areaId, locationId, AdjustType.INCREASE, 5, false, "remark"))
        );
        StockAdjustOrderResponse created = stockAdjustOrderService.create(request);

        assertEquals(skuId, created.items().get(0).skuId());
        assertEquals("SKU-" + suffix, created.items().get(0).skuCode());
    }

    @Test
    void countTaskProfitAppliesDiffAndRestoresLocation() {
        receiveInto(100);
        // The system always mints its own count number; a client-supplied value must be ignored.
        String clientSuppliedCountNo = "CNT-" + suffix + "-" + System.nanoTime();
        StockCountTaskResponse created = stockCountTaskService.create(new CreateStockCountTaskRequest(clientSuppliedCountNo, warehouseId, areaId, locationId, "annual count"));
        assertNotEquals(clientSuppliedCountNo, created.countNo());
        StockCountTaskResponse started = stockCountTaskService.start(created.id());

        assertEquals(StockCountTaskStatus.COUNTING, started.status());
        assertEquals(1, started.items().size());
        assertEquals(100, started.items().get(0).bookOnHandQty());
        assertEquals(LocationStatus.COUNTING, warehouseLocationService.getDetail(locationId).status());

        stockCountTaskService.record(created.id(), new UpdateStockCountItemsRequest(
                List.of(new StockCountItemActualRequest(started.items().get(0).id(), 120, "counted higher"))
        ));
        StockCountTaskResponse completed = stockCountTaskService.complete(created.id());

        assertEquals(StockCountTaskStatus.COMPLETED, completed.status());
        assertEquals(120, fetchInventory().quantity());
        assertEquals(LocationStatus.ENABLED, warehouseLocationService.getDetail(locationId).status());

        StockMovementResponse movement = findMovement(created.countNo(), OperationType.STOCK_COUNT_PROFIT);
        assertEquals(MovementType.COUNT, movement.type());
        assertEquals(100, movement.beforeQuantity());
        assertEquals(120, movement.afterQuantity());
    }

    @Test
    void countTaskLossAppliesDiffAndRestoresLocation() {
        receiveInto(100);
        String clientSuppliedCountNo = "CNT-" + suffix + "-" + System.nanoTime();
        StockCountTaskResponse created = stockCountTaskService.create(new CreateStockCountTaskRequest(clientSuppliedCountNo, warehouseId, areaId, locationId, "annual count"));
        assertNotEquals(clientSuppliedCountNo, created.countNo());
        StockCountTaskResponse started = stockCountTaskService.start(created.id());

        stockCountTaskService.record(created.id(), new UpdateStockCountItemsRequest(
                List.of(new StockCountItemActualRequest(started.items().get(0).id(), 60, "counted lower"))
        ));
        StockCountTaskResponse completed = stockCountTaskService.complete(created.id());

        assertEquals(StockCountTaskStatus.COMPLETED, completed.status());
        assertEquals(60, fetchInventory().quantity());
        assertEquals(LocationStatus.ENABLED, warehouseLocationService.getDetail(locationId).status());

        StockMovementResponse movement = findMovement(created.countNo(), OperationType.STOCK_COUNT_LOSS);
        assertEquals(100, movement.beforeQuantity());
        assertEquals(60, movement.afterQuantity());
    }

    @Test
    void countTaskCannotCompleteWithUnrecordedItems() {
        receiveInto(100);
        String countNo = "CNT-" + suffix + "-" + System.nanoTime();
        StockCountTaskResponse created = stockCountTaskService.create(new CreateStockCountTaskRequest(countNo, warehouseId, areaId, locationId, "annual count"));
        stockCountTaskService.start(created.id());

        assertThrows(BusinessException.class, () -> stockCountTaskService.complete(created.id()));

        assertEquals(LocationStatus.COUNTING, warehouseLocationService.getDetail(locationId).status());
        assertEquals(100, fetchInventory().quantity());
    }
}
