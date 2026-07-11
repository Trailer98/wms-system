package com.example.wms.admin;

import com.example.wms.admin.service.InventoryService;
import com.example.wms.admin.service.SkuService;
import com.example.wms.admin.service.StockAdjustOrderService;
import com.example.wms.admin.service.SysDictService;
import com.example.wms.admin.service.WarehouseAreaService;
import com.example.wms.admin.service.WarehouseLocationService;
import com.example.wms.admin.service.WarehouseService;
import com.example.wms.admin.view.dto.CreateSkuRequest;
import com.example.wms.admin.view.dto.CreateStockAdjustOrderRequest;
import com.example.wms.admin.view.dto.CreateSysDictItemRequest;
import com.example.wms.admin.view.dto.CreateWarehouseAreaRequest;
import com.example.wms.admin.view.dto.CreateWarehouseLocationRequest;
import com.example.wms.admin.view.dto.CreateWarehouseRequest;
import com.example.wms.admin.view.dto.SkuResponse;
import com.example.wms.admin.view.dto.StockAdjustOrderItemRequest;
import com.example.wms.admin.view.dto.StockAdjustOrderResponse;
import com.example.wms.admin.view.dto.StockMovementQuery;
import com.example.wms.admin.view.dto.StockMovementResponse;
import com.example.wms.admin.view.dto.SysDictItemResponse;
import com.example.wms.admin.view.dto.SysDictItemView;
import com.example.wms.admin.view.dto.UpdateSysDictItemRequest;
import com.example.wms.admin.view.dto.WarehouseAreaResponse;
import com.example.wms.admin.view.dto.WarehouseLocationResponse;
import com.example.wms.admin.view.dto.WarehouseResponse;
import com.example.wms.common.common.BusinessException;
import com.example.wms.common.common.PageResponse;
import com.example.wms.common.enums.AdjustReasonType;
import com.example.wms.common.enums.AdjustType;
import com.example.wms.common.enums.AreaType;
import com.example.wms.common.enums.LocationType;
import com.example.wms.common.enums.OperationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = "spring.datasource.url=jdbc:mysql://localhost:3306/wms_system_test?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Tokyo&useSSL=false&allowPublicKeyRetrieval=true")
class SysDictServiceTest {

    private static final String OPERATION_TYPE_DICT_CODE = "stock_movement_operation_type";
    private static final String BIZ_TYPE_DICT_CODE = "stock_movement_biz_type";

    @Autowired
    private SysDictService sysDictService;
    @Autowired
    private WarehouseService warehouseService;
    @Autowired
    private WarehouseAreaService warehouseAreaService;
    @Autowired
    private WarehouseLocationService warehouseLocationService;
    @Autowired
    private SkuService skuService;
    @Autowired
    private InventoryService inventoryService;
    @Autowired
    private StockAdjustOrderService stockAdjustOrderService;

    private String suffix;

    @BeforeEach
    void setUp() {
        suffix = String.valueOf(System.nanoTime());
    }

    @Test
    void operationTypeDictTypeExistsAfterInit() {
        List<SysDictItemView> items = sysDictService.getItems(OPERATION_TYPE_DICT_CODE);
        assertFalse(items.isEmpty(), "stock_movement_operation_type must be seeded on startup");
    }

    @Test
    void bizTypeDictTypeExistsAfterInit() {
        List<SysDictItemView> items = sysDictService.getItems(BIZ_TYPE_DICT_CODE);
        assertFalse(items.isEmpty(), "stock_movement_biz_type must be seeded on startup");
    }

    @Test
    void commonOperationTypeValuesHaveDictItems() {
        List<String> values = sysDictService.getItems(OPERATION_TYPE_DICT_CODE).stream().map(SysDictItemView::value).toList();
        assertTrue(values.contains(OperationType.INBOUND_RECEIVE.name()));
        assertTrue(values.contains(OperationType.STOCK_ADJUST_INCREASE.name()));
        assertTrue(values.contains(OperationType.STOCK_ADJUST_DECREASE.name()));
        assertTrue(values.contains(OperationType.STOCK_COUNT_PROFIT.name()));
        assertTrue(values.contains(OperationType.STOCK_COUNT_LOSS.name()));
        assertTrue(values.contains(OperationType.TRANSFER_TO_EXCEPTION_OUT.name()));
        assertTrue(values.contains(OperationType.RESTORE_FROM_EXCEPTION_IN.name()));
        assertEquals("库存调整减少", sysDictService.getLabel(OPERATION_TYPE_DICT_CODE, OperationType.STOCK_ADJUST_DECREASE.name()));
    }

    @Test
    void commonBizTypeValuesHaveDictItems() {
        List<String> values = sysDictService.getItems(BIZ_TYPE_DICT_CODE).stream().map(SysDictItemView::value).toList();
        assertTrue(values.contains("INBOUND"));
        assertTrue(values.contains("OUTBOUND"));
        assertTrue(values.contains("ADJUSTMENT"));
        assertTrue(values.contains("COUNT"));
        assertEquals("库存调整", sysDictService.getLabel(BIZ_TYPE_DICT_CODE, "ADJUSTMENT"));
    }

    @Test
    void missingDictItemFallsBackToRawCode() {
        String bogusCode = "NOT_A_REAL_CODE_" + suffix;
        assertEquals(bogusCode, sysDictService.getLabel(OPERATION_TYPE_DICT_CODE, bogusCode));
        assertEquals(bogusCode, sysDictService.getLabel("not_a_real_dict_" + suffix, bogusCode));
        assertNotNull(sysDictService.getItems("not_a_real_dict_" + suffix), "an unknown dictCode must return an empty list, not throw");
        assertTrue(sysDictService.getItems("not_a_real_dict_" + suffix).isEmpty());
    }

    @Test
    void systemSeededItemCannotBeDeleted() {
        SysDictItemResponse seeded = sysDictService.listItemsForAdmin(OPERATION_TYPE_DICT_CODE).stream()
                .filter(item -> item.itemValue().equals(OperationType.STOCK_ADJUST_INCREASE.name()))
                .findFirst()
                .orElseThrow();
        assertTrue(seeded.isSystem());
        assertThrows(BusinessException.class, () -> sysDictService.deleteItem(seeded.id()));
    }

    @Test
    void nonSystemItemCanBeDeleted() {
        SysDictItemResponse created = sysDictService.createItem(new CreateSysDictItemRequest(
                OPERATION_TYPE_DICT_CODE, "CUSTOM_TEST_VALUE_" + suffix, "自定义测试项", null, 999, "info", null, "test"
        ));
        assertFalse(created.isSystem());
        sysDictService.deleteItem(created.id());
        assertTrue(sysDictService.listItemsForAdmin(OPERATION_TYPE_DICT_CODE).stream()
                .noneMatch(item -> item.id().equals(created.id())));
    }

    @Test
    void updatingItemLabelInvalidatesCacheAndIsVisibleImmediately() {
        SysDictItemResponse created = sysDictService.createItem(new CreateSysDictItemRequest(
                OPERATION_TYPE_DICT_CODE, "CACHE_TEST_VALUE_" + suffix, "初始文案", null, 999, "info", null, "cache test"
        ));
        // Populate the cache with the original label.
        assertEquals("初始文案", sysDictService.getLabel(OPERATION_TYPE_DICT_CODE, created.itemValue()));

        sysDictService.updateItem(created.id(), new UpdateSysDictItemRequest("更新后文案", null, 999, "warning", null, "cache test updated"));

        assertEquals("更新后文案", sysDictService.getLabel(OPERATION_TYPE_DICT_CODE, created.itemValue()),
                "label must reflect the update immediately, proving the cache was evicted on write");
        assertEquals("warning", sysDictService.getTagType(OPERATION_TYPE_DICT_CODE, created.itemValue()));

        sysDictService.deleteItem(created.id());
    }

    @Test
    void disabledItemIsExcludedFromBusinessLookupButStillResolvesViaFallback() {
        SysDictItemResponse created = sysDictService.createItem(new CreateSysDictItemRequest(
                OPERATION_TYPE_DICT_CODE, "DISABLE_TEST_VALUE_" + suffix, "停用测试项", null, 999, "info", null, "disable test"
        ));
        sysDictService.changeItemStatus(created.id(), "DISABLED");

        assertTrue(sysDictService.getItems(OPERATION_TYPE_DICT_CODE).stream()
                .noneMatch(item -> item.value().equals(created.itemValue())), "DISABLED items must not appear in the ENABLED-only business list");
        assertEquals("停用测试项", sysDictService.getLabel(OPERATION_TYPE_DICT_CODE, created.itemValue()),
                "a DISABLED item must still resolve its label for historical rows via the fallback DB read");

        sysDictService.deleteItem(created.id());
    }

    @Test
    void stockMovementSearchReturnsOperationTypeAndBizTypeLabels() {
        WarehouseResponse warehouse = warehouseService.create(new CreateWarehouseRequest("WH-DICT-" + suffix, "字典测试仓" + suffix, "test address"));
        SkuResponse sku = skuService.create(new CreateSkuRequest("SKU-DICT-" + suffix, "字典测试SKU" + suffix, "EA", "TEST"));
        WarehouseAreaResponse area = warehouseAreaService.create(new CreateWarehouseAreaRequest(warehouse.id(), "STO-DICT-" + suffix, "存储区", AreaType.STORAGE, 10, null));
        WarehouseLocationResponse location = warehouseLocationService.create(new CreateWarehouseLocationRequest(warehouse.id(), area.id(), "A-DICT-" + suffix, "A-01", LocationType.NORMAL, 1000, true, 1, null));

        String adjustNo = "ADJ-DICT-" + suffix;
        CreateStockAdjustOrderRequest request = new CreateStockAdjustOrderRequest(
                adjustNo, warehouse.id(), AdjustReasonType.FOUND, "dict label test",
                List.of(new StockAdjustOrderItemRequest(null, sku.id(), area.id(), location.id(), AdjustType.INCREASE, 10, true, "found stock"))
        );
        StockAdjustOrderResponse created = stockAdjustOrderService.create(request);
        stockAdjustOrderService.submit(created.id());
        StockAdjustOrderResponse confirmed = stockAdjustOrderService.confirm(created.id());

        StockMovementQuery query = new StockMovementQuery();
        query.setBusinessNo(confirmed.adjustNo());
        PageResponse<StockMovementResponse> movements = inventoryService.searchTransactions(query);
        assertFalse(movements.records().isEmpty());

        StockMovementResponse movement = movements.records().get(0);
        assertEquals("ADJUSTMENT", movement.bizType());
        assertEquals("库存调整", movement.bizTypeLabel());
        assertEquals("STOCK_ADJUST_INCREASE", movement.operationType().name());
        assertEquals("库存调整增加", movement.operationTypeLabel());
        assertNotNull(movement.operationTypeTagType());
    }
}
