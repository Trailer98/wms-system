package com.example.wms.admin;

import com.example.wms.admin.aspect.OperationLogContext;
import com.example.wms.admin.service.InboundOrderService;
import com.example.wms.admin.service.InventoryService;
import com.example.wms.admin.service.SkuService;
import com.example.wms.admin.service.StockCountTaskService;
import com.example.wms.admin.service.WarehouseAreaService;
import com.example.wms.admin.service.WarehouseLocationService;
import com.example.wms.admin.service.WarehouseService;
import com.example.wms.admin.view.dto.CreateInboundOrderRequest;
import com.example.wms.admin.view.dto.CreateSkuRequest;
import com.example.wms.admin.view.dto.CreateWarehouseAreaRequest;
import com.example.wms.admin.view.dto.CreateWarehouseLocationRequest;
import com.example.wms.admin.view.dto.CreateWarehouseRequest;
import com.example.wms.admin.view.dto.InboundOrderItemRequest;
import com.example.wms.admin.view.dto.InventoryQuery;
import com.example.wms.admin.view.dto.InventoryResponse;
import com.example.wms.admin.view.dto.SkuResponse;
import com.example.wms.admin.view.dto.StockCountItemActualRequest;
import com.example.wms.admin.view.dto.StockCountTaskResponse;
import com.example.wms.admin.view.dto.UpdateStockCountItemsRequest;
import com.example.wms.admin.view.dto.WarehouseAreaResponse;
import com.example.wms.admin.view.dto.WarehouseLocationResponse;
import com.example.wms.admin.view.dto.WarehouseResponse;
import com.example.wms.common.common.PageResponse;
import com.example.wms.common.enums.AreaType;
import com.example.wms.common.enums.LocationType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the "operation log must carry the backend-generated business number, not the
 * request-supplied one" fix: {@code @SysOperationLog(bizNo = ...)} on the four order-creation
 * endpoints used to read {@code #request.xxxNo()} (always blank/forged, since the client never
 * generates numbers) instead of {@code #result.data().xxxNo()} (the number the service actually
 * minted and saved). Also covers the non-create endpoints that previously had no bizNo expression
 * at all and silently logged nothing.
 * <p>
 * Log rows are written by {@code SysOperationLogAsyncService} on a separate executor thread, so
 * every assertion here polls {@code /operation-logs} for up to a few seconds instead of asserting
 * immediately after the triggering call returns.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.datasource.url=jdbc:mysql://localhost:3306/wms_system_test?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Tokyo&useSSL=false&allowPublicKeyRetrieval=true"
)
class OperationLogBizNoTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

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
    private StockCountTaskService stockCountTaskService;

    private String suffix;
    private String adminToken;
    private Long warehouseId;
    private Long areaId;
    private Long locationId;
    private Long skuId;

    @BeforeEach
    void setUp() {
        suffix = String.valueOf(System.nanoTime());
        adminToken = login("admin", "admin123");

        WarehouseResponse warehouse = warehouseService.create(new CreateWarehouseRequest("WH-" + suffix, "测试仓" + suffix, "test address"));
        warehouseId = warehouse.id();

        SkuResponse sku = skuService.create(new CreateSkuRequest("SKU-" + suffix, "测试SKU" + suffix, "EA", "TEST"));
        skuId = sku.id();

        WarehouseAreaResponse area = warehouseAreaService.create(new CreateWarehouseAreaRequest(warehouseId, "STO-" + suffix, "存储区", AreaType.STORAGE, 10, null));
        areaId = area.id();

        WarehouseLocationResponse location = warehouseLocationService.create(new CreateWarehouseLocationRequest(warehouseId, areaId, "A-01-" + suffix, "A-01", LocationType.NORMAL, 1000, true, 1, null));
        locationId = location.id();
    }

    // ---- 1. create inbound order: bizNo/bizId in the log must come from the response, not the request ----
    @Test
    void createInboundOrderLogsGeneratedOrderNo() {
        String forgedNo = "FORGED-IN-" + suffix;
        Map<String, Object> body = Map.of(
                "orderNo", forgedNo,
                "warehouseId", warehouseId,
                "items", List.of(Map.of("skuId", skuId, "quantity", 10, "areaId", areaId, "locationId", locationId))
        );

        JsonNode data = postWithToken("/inbound-orders", body).get("data");
        String orderNo = data.get("orderNo").asText();
        long id = data.get("id").asLong();
        assertNotEquals(forgedNo, orderNo, "the system must mint its own inbound order number");

        JsonNode logRow = waitForLogByBizNo(orderNo);
        assertEquals("INBOUND_ORDER", logRow.get("bizType").asText());
        assertEquals(orderNo, logRow.get("bizNo").asText());
        assertEquals(id, logRow.get("bizId").asLong());
        assertNotEquals(forgedNo, logRow.get("bizNo").asText(), "log must never carry a client-forged number");
    }

    // ---- 2. create outbound order ----
    @Test
    void createOutboundOrderLogsGeneratedOrderNo() {
        String forgedNo = "FORGED-OUT-" + suffix;
        Map<String, Object> body = Map.of(
                "orderNo", forgedNo,
                "warehouseId", warehouseId,
                "items", List.of(Map.of("skuId", skuId, "quantity", 5))
        );

        JsonNode data = postWithToken("/outbound-orders", body).get("data");
        String orderNo = data.get("orderNo").asText();
        long id = data.get("id").asLong();
        assertNotEquals(forgedNo, orderNo);

        JsonNode logRow = waitForLogByBizNo(orderNo);
        assertEquals("OUTBOUND_ORDER", logRow.get("bizType").asText());
        assertEquals(orderNo, logRow.get("bizNo").asText());
        assertEquals(id, logRow.get("bizId").asLong());
    }

    // ---- 3. create stock adjust order + malicious forged adjustNo must be ignored everywhere ----
    @Test
    void createStockAdjustOrderLogsGeneratedAdjustNoIgnoringForgedRequestValue() {
        receiveInto(50);
        Long inventoryId = fetchInventory().id();

        String forgedNo = "FORGED-ADJ-" + suffix;
        Map<String, Object> body = Map.of(
                "adjustNo", forgedNo,
                "warehouseId", warehouseId,
                "reasonType", "DATA_ERROR",
                "reason", "test",
                "items", List.of(Map.of(
                        "inventoryId", inventoryId,
                        "adjustType", "INCREASE",
                        "adjustQty", 5,
                        "allowCreateInventory", false
                ))
        );

        JsonNode data = postWithToken("/stock-adjust-orders", body).get("data");
        String adjustNo = data.get("adjustNo").asText();
        long id = data.get("id").asLong();
        assertNotEquals(forgedNo, adjustNo);

        JsonNode logRow = waitForLogByBizNo(adjustNo);
        assertEquals("STOCK_ADJUST_ORDER", logRow.get("bizType").asText());
        assertEquals(adjustNo, logRow.get("bizNo").asText());
        assertEquals(id, logRow.get("bizId").asLong());
        assertNotEquals(forgedNo, logRow.get("bizNo").asText());
    }

    // ---- 4. create stock count task ----
    @Test
    void createStockCountTaskLogsGeneratedCountNo() {
        receiveInto(20);

        String forgedNo = "FORGED-CNT-" + suffix;
        Map<String, Object> body = Map.of(
                "countNo", forgedNo,
                "warehouseId", warehouseId,
                "areaId", areaId,
                "locationId", locationId
        );

        JsonNode data = postWithToken("/stock-count-tasks", body).get("data");
        String countNo = data.get("countNo").asText();
        long id = data.get("id").asLong();
        assertNotEquals(forgedNo, countNo);

        JsonNode logRow = waitForLogByBizNo(countNo);
        assertEquals("STOCK_COUNT_TASK", logRow.get("bizType").asText());
        assertEquals(countNo, logRow.get("bizNo").asText());
        assertEquals(id, logRow.get("bizId").asLong());
    }

    // ---- 6. stock adjust confirm: request only carries {id}, log must still resolve adjustNo ----
    @Test
    void confirmStockAdjustOrderLogsAdjustNoFromIdOnlyRequest() {
        receiveInto(50);
        Long inventoryId = fetchInventory().id();

        Map<String, Object> createBody = Map.of(
                "warehouseId", warehouseId,
                "reasonType", "DATA_ERROR",
                "reason", "test",
                "items", List.of(Map.of(
                        "inventoryId", inventoryId,
                        "adjustType", "INCREASE",
                        "adjustQty", 5,
                        "allowCreateInventory", false
                ))
        );
        JsonNode created = postWithToken("/stock-adjust-orders", createBody).get("data");
        long id = created.get("id").asLong();
        String adjustNo = created.get("adjustNo").asText();

        postWithToken("/stock-adjust-orders/" + id + "/submit", null);
        JsonNode confirmed = postWithToken("/stock-adjust-orders/" + id + "/confirm", null).get("data");
        assertEquals(adjustNo, confirmed.get("adjustNo").asText());

        JsonNode logRow = waitForLogByBizNoAndOperationType(adjustNo, "确认库存调整单");
        assertEquals("STOCK_ADJUST_ORDER", logRow.get("bizType").asText());
        assertEquals(id, logRow.get("bizId").asLong());
    }

    // ---- 7. stock count complete: request only carries {id}, log must still resolve countNo ----
    @Test
    void completeStockCountTaskLogsCountNoFromIdOnlyRequest() {
        receiveInto(100);

        StockCountTaskResponse created = stockCountTaskService.create(
                new com.example.wms.admin.view.dto.CreateStockCountTaskRequest(null, warehouseId, areaId, locationId, "test count"));
        StockCountTaskResponse started = stockCountTaskService.start(created.id());
        stockCountTaskService.record(created.id(), new UpdateStockCountItemsRequest(
                List.of(new StockCountItemActualRequest(started.items().get(0).id(), 100, "matches book qty"))));

        JsonNode completed = postWithToken("/stock-count-tasks/" + created.id() + "/complete", null).get("data");
        assertEquals(created.countNo(), completed.get("countNo").asText());

        JsonNode logRow = waitForLogByBizNoAndOperationType(created.countNo(), "完成库存盘点");
        assertEquals("STOCK_COUNT_TASK", logRow.get("bizType").asText());
        assertEquals(created.id(), logRow.get("bizId").asLong());
    }

    // ---- 8. a business exception must still produce a (failed) log row, and must not be swallowed ----
    @Test
    void failedConfirmOnUnsubmittedOrderStillWritesFailedLogAndPropagatesException() {
        receiveInto(50);
        Long inventoryId = fetchInventory().id();

        Map<String, Object> createBody = Map.of(
                "warehouseId", warehouseId,
                "reasonType", "DATA_ERROR",
                "reason", "test",
                "items", List.of(Map.of(
                        "inventoryId", inventoryId,
                        "adjustType", "INCREASE",
                        "adjustQty", 5,
                        "allowCreateInventory", false
                ))
        );
        JsonNode created = postWithToken("/stock-adjust-orders", createBody).get("data");
        long id = created.get("id").asLong();

        // Confirming before submit violates the state machine (StockAdjustOrderService.confirm
        // requires SUBMITTED) -> BusinessException -> GlobalExceptionHandler maps it to HTTP 400.
        ResponseEntity<String> response = exchange(HttpMethod.POST, "/stock-adjust-orders/" + id + "/confirm", null, adminToken);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(), "the exception must still propagate to the client, not be swallowed by the aspect");

        JsonNode logRow = waitForFailedLogContainingUri("确认库存调整单", "/stock-adjust-orders/" + id + "/confirm");
        assertFalse(logRow.get("success").asBoolean());
        assertNotNull(logRow.get("errorMessage").asText());
        assertFalse(logRow.get("errorMessage").asText().isBlank(), "request context (uri/method/error) must still be recorded even without a bizNo");
    }

    // ---- 9. OperationLogContext must never leak state across requests ----
    @Test
    void operationLogContextIsClearedAfterGetAndClear() {
        OperationLogContext.setBizNo("CTX-" + suffix);
        OperationLogContext.setBizId(42L);
        OperationLogContext.setBizType("TEST_TYPE");
        OperationLogContext.setExtra("k", "v");

        OperationLogContext.Holder first = OperationLogContext.getAndClear();
        assertEquals("CTX-" + suffix, first.getBizNo());
        assertEquals(42L, first.getBizId());
        assertEquals("TEST_TYPE", first.getBizType());
        assertEquals("v", first.getExtra().get("k"));

        // Second read on the same thread without setting anything must come back empty: proves the
        // ThreadLocal was actually removed, not just nulled out, so a later request/task on this
        // thread never inherits a previous request's business number.
        OperationLogContext.Holder second = OperationLogContext.getAndClear();
        assertNull(second.getBizNo());
        assertNull(second.getBizId());
        assertNull(second.getBizType());
        assertTrue(second.getExtra().isEmpty());
    }

    // ---------------------------------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------------------------------

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

    private JsonNode waitForLogByBizNo(String bizNo) {
        return waitFor(() -> {
            JsonNode records = queryLogs(Map.of("bizNo", bizNo));
            return records.size() > 0 ? records.get(0) : null;
        }, "no operation log found for bizNo=" + bizNo);
    }

    // bizNo alone is enough to select the row: it's a unique, system-minted number, and the query
    // result is already ordered by createTime desc, so the most recent operation against that
    // order (the one this test just triggered) is always records[0]. operationType is compared
    // client-side (plain Java String equality) rather than sent as a query param, since pushing
    // Chinese text through UriComponentsBuilder + RestTemplate's string-URI overload mangles the
    // encoding and the server-side `like` filter then matches nothing.
    private JsonNode waitForLogByBizNoAndOperationType(String bizNo, String operationType) {
        return waitFor(() -> {
            JsonNode records = queryLogs(Map.of("bizNo", bizNo));
            for (JsonNode row : records) {
                if (operationType.equals(row.get("operationType").asText())) {
                    return row;
                }
            }
            return null;
        }, "no operation log found for bizNo=" + bizNo + ", operationType=" + operationType);
    }

    private JsonNode waitForFailedLogContainingUri(String operationType, String uriFragment) {
        return waitFor(() -> {
            JsonNode records = queryLogs(Map.of("pageSize", "50"));
            for (JsonNode row : records) {
                if (operationType.equals(row.get("operationType").asText())
                        && row.get("requestUri").asText().contains(uriFragment)
                        && !row.get("success").asBoolean()) {
                    return row;
                }
            }
            return null;
        }, "no failed operation log found for operationType=" + operationType + " uri containing " + uriFragment);
    }

    private JsonNode queryLogs(Map<String, String> params) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/operation-logs");
        params.forEach(builder::queryParam);
        JsonNode root = getWithToken(builder.toUriString());
        return root.get("data").get("records");
    }

    private interface Poll {
        JsonNode tryGet();
    }

    private JsonNode waitFor(Poll poll, String failureMessage) {
        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline) {
            JsonNode result = poll.tryGet();
            if (result != null) {
                return result;
            }
            sleep(200);
        }
        throw new AssertionError(failureMessage);
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    private String login(String username, String password) {
        ResponseEntity<String> response = restTemplate.postForEntity(
                url("/auth/login"), Map.of("username", username, "password", password), String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        JsonNode root = readTree(response.getBody());
        assertEquals(200, root.get("code").asInt());
        return root.get("data").get("token").asText();
    }

    private JsonNode getWithToken(String path) {
        ResponseEntity<String> response = exchange(HttpMethod.GET, path, null, adminToken);
        assertEquals(HttpStatus.OK, response.getStatusCode(), "GET " + path + " failed: " + response.getBody());
        return readTree(response.getBody());
    }

    private JsonNode postWithToken(String path, Object body) {
        ResponseEntity<String> response = exchange(HttpMethod.POST, path, body, adminToken);
        assertEquals(HttpStatus.OK, response.getStatusCode(), "POST " + path + " failed: " + response.getBody());
        return readTree(response.getBody());
    }

    private ResponseEntity<String> exchange(HttpMethod method, String path, Object body, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        if (body != null) {
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        }
        return restTemplate.exchange(url(path), method, new HttpEntity<>(body, headers), String.class);
    }

    private JsonNode readTree(String json) {
        try {
            return OBJECT_MAPPER.readTree(json);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String url(String path) {
        return "http://localhost:" + port + "/api" + path;
    }
}
