package com.example.wms.admin;

import com.example.wms.admin.service.InboundOrderService;
import com.example.wms.admin.service.InventoryService;
import com.example.wms.admin.service.OutboundOrderService;
import com.example.wms.admin.service.SkuService;
import com.example.wms.admin.service.WarehouseAreaService;
import com.example.wms.admin.service.WarehouseLocationService;
import com.example.wms.admin.service.WarehouseService;
import com.example.wms.admin.service.WmsExceptionEventService;
import com.example.wms.admin.view.dto.CreateInboundOrderRequest;
import com.example.wms.admin.view.dto.CreateOutboundOrderRequest;
import com.example.wms.admin.view.dto.CreateSkuRequest;
import com.example.wms.admin.view.dto.CreateWarehouseAreaRequest;
import com.example.wms.admin.view.dto.CreateWarehouseLocationRequest;
import com.example.wms.admin.view.dto.CreateWarehouseRequest;
import com.example.wms.admin.view.dto.InboundOrderItemRequest;
import com.example.wms.admin.view.dto.InboundOrderResponse;
import com.example.wms.admin.view.dto.InventoryQuery;
import com.example.wms.admin.view.dto.InventoryResponse;
import com.example.wms.admin.view.dto.OrderItemRequest;
import com.example.wms.admin.view.dto.OutboundOrderResponse;
import com.example.wms.admin.view.dto.SkuResponse;
import com.example.wms.admin.view.dto.StockMovementQuery;
import com.example.wms.admin.view.dto.StockMovementResponse;
import com.example.wms.admin.view.dto.UpdateInboundOrderRequest;
import com.example.wms.admin.view.dto.UpdateOutboundOrderRequest;
import com.example.wms.admin.view.dto.UpdateStatusRequest;
import com.example.wms.admin.view.dto.UpdateWarehouseLocationRequest;
import com.example.wms.admin.view.dto.WarehouseAreaResponse;
import com.example.wms.admin.view.dto.WarehouseLocationResponse;
import com.example.wms.admin.view.dto.WarehouseResponse;
import com.example.wms.admin.view.dto.WmsExceptionEventQuery;
import com.example.wms.admin.view.dto.WmsExceptionEventResponse;
import com.example.wms.common.common.BusinessException;
import com.example.wms.common.common.PageResponse;
import com.example.wms.common.enums.AreaType;
import com.example.wms.common.enums.ExceptionType;
import com.example.wms.common.enums.InboundOrderStatus;
import com.example.wms.common.enums.LocationType;
import com.example.wms.common.enums.MovementType;
import com.example.wms.common.enums.OperationType;
import com.example.wms.common.enums.OutboundOrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Each service call below runs in its own top-level transaction, exactly like a real request.
// This intentionally does not wrap the whole test method in one Spring-test-managed transaction:
// the all-or-nothing rollback behavior under test (case4) only actually happens on a real
// transaction boundary, so nesting it inside one outer transaction would hide the rollback
// (the same connection would still see its own uncommitted writes until the outer one ends).
@SpringBootTest(properties = "spring.datasource.url=jdbc:mysql://localhost:3306/wms_system_test?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true")
class AreaLocationInventoryFlowTest {

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
    private OutboundOrderService outboundOrderService;
    @Autowired
    private InventoryService inventoryService;
    @Autowired
    private WmsExceptionEventService wmsExceptionEventService;

    private String suffix;
    private Long warehouseId;
    private Long storageAreaId;
    private Long exceptionAreaId;
    private Long storageLocationId;
    private Long exceptionLocationId;
    private Long skuId;

    @BeforeEach
    void setUp() {
        suffix = String.valueOf(System.nanoTime());

        WarehouseResponse warehouse = warehouseService.create(new CreateWarehouseRequest("WH-" + suffix, "测试仓" + suffix, "test address"));
        warehouseId = warehouse.id();

        SkuResponse sku = skuService.create(new CreateSkuRequest("SKU-" + suffix, "测试SKU" + suffix, "EA", "TEST"));
        skuId = sku.id();

        WarehouseAreaResponse storageArea = warehouseAreaService.create(new CreateWarehouseAreaRequest(warehouseId, "STO-" + suffix, "存储区", AreaType.STORAGE, 10, null));
        storageAreaId = storageArea.id();

        WarehouseAreaResponse pickingArea = warehouseAreaService.create(new CreateWarehouseAreaRequest(warehouseId, "PICK-" + suffix, "拣货区", AreaType.PICKING, 1, null));

        WarehouseAreaResponse exceptionArea = warehouseAreaService.create(new CreateWarehouseAreaRequest(warehouseId, "EXC-" + suffix, "异常区", AreaType.EXCEPTION, 1, null));
        exceptionAreaId = exceptionArea.id();

        WarehouseLocationResponse storageLocation = warehouseLocationService.create(new CreateWarehouseLocationRequest(warehouseId, storageAreaId, "A-01-" + suffix, "A-01", LocationType.NORMAL, 100, true, 1, null));
        storageLocationId = storageLocation.id();

        warehouseLocationService.create(new CreateWarehouseLocationRequest(warehouseId, pickingArea.id(), "P-01-" + suffix, "P-01", LocationType.NORMAL, 100, true, 1, null));

        WarehouseLocationResponse exceptionLocation = warehouseLocationService.create(new CreateWarehouseLocationRequest(warehouseId, exceptionAreaId, "E-01-" + suffix, "E-01", LocationType.NORMAL, 100, true, 1, null));
        exceptionLocationId = exceptionLocation.id();
    }

    @Test
    void case1_inboundToNormalLocation() {
        InboundOrderResponse order = createAndReceiveInbound(storageAreaId, storageLocationId, 100);

        InventoryResponse inv = fetchInventory(storageLocationId);
        assertEquals(100, inv.quantity());
        assertEquals(100, inv.availableQuantity());

        StockMovementResponse movement = findMovement(order.orderNo(), MovementType.INBOUND);
        assertEquals(OperationType.ON_HAND_INCREASE, movement.operationType());
        assertEquals(0, movement.beforeQuantity());
        assertEquals(100, movement.afterQuantity());
        assertEquals(0, movement.beforeReservedQuantity());
        assertEquals(0, movement.afterReservedQuantity());
        assertEquals(0, movement.beforeFrozenQuantity());
        assertEquals(0, movement.afterFrozenQuantity());
        assertEquals(0, movement.beforeAvailableQuantity());
        assertEquals(100, movement.afterAvailableQuantity());
    }

    @Test
    void case2_lockOutbound() {
        createAndReceiveInbound(storageAreaId, storageLocationId, 100);

        OutboundOrderResponse created = outboundOrderService.create(new CreateOutboundOrderRequest("OUT-" + suffix, warehouseId, null, List.of(new OrderItemRequest(skuId, 30))));
        OutboundOrderResponse locked = outboundOrderService.lock(created.id());

        assertEquals(OutboundOrderStatus.LOCKED, locked.status());
        InventoryResponse inv = fetchInventory(storageLocationId);
        assertEquals(100, inv.quantity());
        assertEquals(30, inv.reservedQuantity());
        assertEquals(70, inv.availableQuantity());
        assertEquals(1, locked.allocations().size());
        assertEquals(storageLocationId, locked.allocations().get(0).locationId());

        StockMovementResponse movement = findMovement(created.orderNo(), MovementType.LOCK);
        assertEquals(OperationType.STOCK_LOCK, movement.operationType());
        assertEquals(100, movement.beforeQuantity());
        assertEquals(100, movement.afterQuantity());
        assertEquals(0, movement.beforeReservedQuantity());
        assertEquals(30, movement.afterReservedQuantity());
        assertEquals(100, movement.beforeAvailableQuantity());
        assertEquals(70, movement.afterAvailableQuantity());
    }

    @Test
    void case3_confirmShip() {
        createAndReceiveInbound(storageAreaId, storageLocationId, 100);
        OutboundOrderResponse created = outboundOrderService.create(new CreateOutboundOrderRequest("OUT-" + suffix, warehouseId, null, List.of(new OrderItemRequest(skuId, 30))));
        outboundOrderService.lock(created.id());
        OutboundOrderResponse shipped = outboundOrderService.ship(created.id());

        assertEquals(OutboundOrderStatus.SHIPPED, shipped.status());
        InventoryResponse inv = fetchInventory(storageLocationId);
        assertEquals(70, inv.quantity());
        assertEquals(0, inv.reservedQuantity());
        assertEquals(70, inv.availableQuantity());

        StockMovementResponse movement = findMovement(created.orderNo(), MovementType.OUTBOUND);
        assertEquals(OperationType.ON_HAND_DECREASE, movement.operationType());
        assertEquals(100, movement.beforeQuantity());
        assertEquals(70, movement.afterQuantity());
        assertEquals(30, movement.beforeReservedQuantity());
        assertEquals(0, movement.afterReservedQuantity());
        assertEquals(70, movement.beforeAvailableQuantity());
        assertEquals(70, movement.afterAvailableQuantity());
    }

    @Test
    void case4_insufficientInventoryLockFails() {
        createAndReceiveInbound(storageAreaId, storageLocationId, 70);
        OutboundOrderResponse created = outboundOrderService.create(new CreateOutboundOrderRequest("OUT-" + suffix, warehouseId, null, List.of(new OrderItemRequest(skuId, 80))));

        assertThrows(BusinessException.class, () -> outboundOrderService.lock(created.id()));

        InventoryResponse inv = fetchInventory(storageLocationId);
        assertEquals(70, inv.quantity());
        assertEquals(0, inv.reservedQuantity());

        PageResponse<StockMovementResponse> movements = inventoryService.searchTransactions(transactionQuery(created.orderNo()));
        assertTrue(movements.records().isEmpty(), "no stock movement should be written when locking fails");

        WmsExceptionEventQuery query = new WmsExceptionEventQuery();
        query.setBizNo(created.orderNo());
        PageResponse<WmsExceptionEventResponse> events = wmsExceptionEventService.search(query);
        assertTrue(events.records().stream().anyMatch(e -> e.exceptionType() == ExceptionType.INVENTORY_NOT_ENOUGH));

        WmsExceptionEventResponse event = events.records().stream()
                .filter(e -> e.exceptionType() == ExceptionType.INVENTORY_NOT_ENOUGH)
                .findFirst().orElseThrow();
        assertEquals("OPEN", event.status());
        WmsExceptionEventResponse handled = wmsExceptionEventService.markHandled(event.id());
        assertEquals("HANDLED", handled.status());
    }

    @Test
    void case5_cancelLockedOrderReleases() {
        createAndReceiveInbound(storageAreaId, storageLocationId, 100);
        OutboundOrderResponse created = outboundOrderService.create(new CreateOutboundOrderRequest("OUT-" + suffix, warehouseId, null, List.of(new OrderItemRequest(skuId, 20))));
        outboundOrderService.lock(created.id());
        OutboundOrderResponse cancelled = outboundOrderService.cancel(created.id());

        assertEquals(OutboundOrderStatus.CANCELLED, cancelled.status());
        InventoryResponse inv = fetchInventory(storageLocationId);
        assertEquals(100, inv.quantity());
        assertEquals(0, inv.reservedQuantity());
        assertEquals(100, inv.availableQuantity());

        PageResponse<StockMovementResponse> movements = inventoryService.searchTransactions(transactionQuery(created.orderNo()));
        assertTrue(movements.records().stream().anyMatch(m -> m.type() == MovementType.UNLOCK));

        StockMovementResponse unlockMovement = findMovement(created.orderNo(), MovementType.UNLOCK);
        assertEquals(OperationType.STOCK_UNLOCK, unlockMovement.operationType());
    }

    @Test
    void case6_exceptionAreaCannotBeAllocatedForOutbound() {
        createAndReceiveInbound(exceptionAreaId, exceptionLocationId, 10);

        OutboundOrderResponse created = outboundOrderService.create(new CreateOutboundOrderRequest("OUT-" + suffix, warehouseId, null, List.of(new OrderItemRequest(skuId, 5))));
        assertThrows(BusinessException.class, () -> outboundOrderService.lock(created.id()));
    }

    @Test
    void case7_countingLocationBlocksInboundAndOutbound() {
        createAndReceiveInbound(storageAreaId, storageLocationId, 50);

        warehouseLocationService.updateStatus(storageLocationId, new UpdateStatusRequest("COUNTING"));

        CreateInboundOrderRequest inboundRequest = new CreateInboundOrderRequest(
                "IN2-" + suffix, warehouseId, null,
                List.of(new InboundOrderItemRequest(skuId, 10, storageAreaId, storageLocationId))
        );
        InboundOrderResponse inboundOrder = inboundOrderService.create(inboundRequest);
        assertThrows(BusinessException.class, () -> inboundOrderService.receive(inboundOrder.id()));

        OutboundOrderResponse created = outboundOrderService.create(new CreateOutboundOrderRequest("OUT-" + suffix, warehouseId, null, List.of(new OrderItemRequest(skuId, 10))));
        assertThrows(BusinessException.class, () -> outboundOrderService.lock(created.id()));
    }

    @Test
    void case8_locationCapacityLimit() {
        warehouseLocationService.update(storageLocationId, new UpdateWarehouseLocationRequest("A-01", LocationType.NORMAL, 100, true, 1, null));
        createAndReceiveInbound(storageAreaId, storageLocationId, 90);

        CreateInboundOrderRequest request = new CreateInboundOrderRequest(
                "IN2-" + suffix, warehouseId, null,
                List.of(new InboundOrderItemRequest(skuId, 20, storageAreaId, storageLocationId))
        );
        InboundOrderResponse order = inboundOrderService.create(request);
        assertThrows(BusinessException.class, () -> inboundOrderService.receive(order.id()));

        InventoryResponse inv = fetchInventory(storageLocationId);
        assertEquals(90, inv.quantity());
    }

    @Test
    void case9_editInboundOrderBeforeReceiveOnly() {
        InboundOrderResponse created = inboundOrderService.create(new CreateInboundOrderRequest(
                "IN-EDIT-" + suffix, warehouseId, null,
                List.of(new InboundOrderItemRequest(skuId, 10, storageAreaId, storageLocationId))
        ));

        InboundOrderResponse updated = inboundOrderService.update(created.id(), new UpdateInboundOrderRequest(
                null, List.of(new InboundOrderItemRequest(skuId, 40, storageAreaId, storageLocationId))
        ));
        assertEquals(1, updated.items().size());
        assertEquals(40, updated.items().get(0).quantity());

        InboundOrderResponse received = inboundOrderService.receive(created.id());
        assertEquals(InboundOrderStatus.RECEIVED, received.status());
        InventoryResponse inv = fetchInventory(storageLocationId);
        assertEquals(40, inv.quantity());

        assertThrows(BusinessException.class, () -> inboundOrderService.update(created.id(), new UpdateInboundOrderRequest(
                null, List.of(new InboundOrderItemRequest(skuId, 5, storageAreaId, storageLocationId))
        )));
    }

    @Test
    void case10_editOutboundOrderBeforeLockOnly() {
        createAndReceiveInbound(storageAreaId, storageLocationId, 100);
        OutboundOrderResponse created = outboundOrderService.create(new CreateOutboundOrderRequest(
                "OUT-EDIT-" + suffix, warehouseId, null, List.of(new OrderItemRequest(skuId, 10))
        ));

        OutboundOrderResponse updated = outboundOrderService.update(created.id(), new UpdateOutboundOrderRequest(
                null, List.of(new OrderItemRequest(skuId, 25))
        ));
        assertEquals(1, updated.items().size());
        assertEquals(25, updated.items().get(0).quantity());

        OutboundOrderResponse locked = outboundOrderService.lock(created.id());
        assertEquals(OutboundOrderStatus.LOCKED, locked.status());
        assertEquals(25, locked.allocations().get(0).lockQty());

        assertThrows(BusinessException.class, () -> outboundOrderService.update(created.id(), new UpdateOutboundOrderRequest(
                null, List.of(new OrderItemRequest(skuId, 1))
        )));
    }

    @Test
    void case11_deleteInboundOrderOnlyBeforeReceive() {
        InboundOrderResponse created = inboundOrderService.create(new CreateInboundOrderRequest(
                "IN-DEL-" + suffix, warehouseId, null,
                List.of(new InboundOrderItemRequest(skuId, 10, storageAreaId, storageLocationId))
        ));
        inboundOrderService.receive(created.id());
        assertThrows(BusinessException.class, () -> inboundOrderService.delete(created.id()));

        InboundOrderResponse deletable = inboundOrderService.create(new CreateInboundOrderRequest(
                "IN-DEL2-" + suffix, warehouseId, null,
                List.of(new InboundOrderItemRequest(skuId, 10, storageAreaId, storageLocationId))
        ));
        inboundOrderService.delete(deletable.id());
        assertThrows(BusinessException.class, () -> inboundOrderService.getDetail(deletable.id()));
    }

    @Test
    void case12_deleteLockedOutboundOrderReleasesLock() {
        createAndReceiveInbound(storageAreaId, storageLocationId, 100);
        OutboundOrderResponse created = outboundOrderService.create(new CreateOutboundOrderRequest(
                "OUT-DEL-" + suffix, warehouseId, null, List.of(new OrderItemRequest(skuId, 30))
        ));
        outboundOrderService.lock(created.id());

        InventoryResponse lockedInv = fetchInventory(storageLocationId);
        assertEquals(30, lockedInv.reservedQuantity());

        outboundOrderService.delete(created.id());

        InventoryResponse afterDelete = fetchInventory(storageLocationId);
        assertEquals(0, afterDelete.reservedQuantity());
        assertEquals(100, afterDelete.availableQuantity());
        assertThrows(BusinessException.class, () -> outboundOrderService.getDetail(created.id()));

        OutboundOrderResponse shippable = outboundOrderService.create(new CreateOutboundOrderRequest(
                "OUT-DEL2-" + suffix, warehouseId, null, List.of(new OrderItemRequest(skuId, 20))
        ));
        outboundOrderService.lock(shippable.id());
        outboundOrderService.ship(shippable.id());
        assertThrows(BusinessException.class, () -> outboundOrderService.delete(shippable.id()));
    }

    private InboundOrderResponse createAndReceiveInbound(Long areaId, Long locationId, int quantity) {
        CreateInboundOrderRequest request = new CreateInboundOrderRequest(
                "IN-" + suffix + "-" + System.nanoTime(), warehouseId, null,
                List.of(new InboundOrderItemRequest(skuId, quantity, areaId, locationId))
        );
        InboundOrderResponse order = inboundOrderService.create(request);
        return inboundOrderService.receive(order.id());
    }

    private InventoryResponse fetchInventory(Long locationId) {
        InventoryQuery query = new InventoryQuery();
        query.setLocationId(locationId);
        PageResponse<InventoryResponse> page = inventoryService.search(query);
        assertEquals(1, page.records().size());
        return page.records().get(0);
    }

    private StockMovementQuery transactionQuery(String businessNo) {
        StockMovementQuery query = new StockMovementQuery();
        query.setBusinessNo(businessNo);
        return query;
    }

    private StockMovementResponse findMovement(String businessNo, MovementType type) {
        PageResponse<StockMovementResponse> movements = inventoryService.searchTransactions(transactionQuery(businessNo));
        return movements.records().stream()
                .filter(m -> m.type() == type)
                .findFirst()
                .orElseThrow(() -> new AssertionError("no " + type + " movement found for business no " + businessNo));
    }
}
