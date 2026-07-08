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
import com.example.wms.admin.view.dto.UpdateStockCountItemsRequest;
import com.example.wms.admin.view.dto.WarehouseAreaResponse;
import com.example.wms.admin.view.dto.WarehouseLocationResponse;
import com.example.wms.admin.view.dto.WarehouseResponse;
import com.example.wms.common.common.BusinessException;
import com.example.wms.common.common.PageResponse;
import com.example.wms.common.enums.AdjustReasonType;
import com.example.wms.common.enums.AreaType;
import com.example.wms.common.enums.AdjustType;
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
