package com.example.wms.admin.aidemo;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/**
 * Direct-JDBC bulk inserter that simulates roughly one calendar year of WMS activity in one shot,
 * for AI tool-calling demos that need enterprise-scale volume rather than the ~100-order sample a
 * Controller/Service-driven seeder would produce at this scale. Deliberately bypasses Controllers,
 * Services, and MyBatis entities entirely — this is a data-generation tool, not a simulation of "a
 * user clicking buttons". Every row is built to satisfy the same invariants the real Service layer
 * would have enforced (guarded against here in {@link Ledger}), since nothing downstream
 * double-checks them at insert time. Fully self-contained on purpose: does not read from or write to
 * any other demo-data mechanism in this codebase, and safety is re-checked on every call (not just at
 * bean-registration time) so it stays callable from tests or any future caller.
 */
@Component
public class EnterpriseYearDemoDataBulkInserter {

    private static final Logger log = LoggerFactory.getLogger(EnterpriseYearDemoDataBulkInserter.class);
    private static final DateTimeFormatter SEQ_DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;
    private static final int BATCH_FLUSH_SIZE = 1000;
    private static final List<String> FORBIDDEN_DATABASE_NAME_SUBSTRINGS = List.of("prod", "production", "online");

    private final JdbcTemplate jdbcTemplate;
    private final Environment environment;

    public EnterpriseYearDemoDataBulkInserter(JdbcTemplate jdbcTemplate, Environment environment) {
        this.jdbcTemplate = jdbcTemplate;
        this.environment = environment;
    }

    public record Summary(
            String batchCode, LocalDate startDate, int days, String scale,
            int warehouses, int areas, int locations, int skus, int suppliers, int customers,
            int inboundOrders, int outboundOrders, int adjustOrders, int countTasks,
            int inventoryRows, int stockMovements, int exceptionEvents, int operationLogs,
            long elapsedMillis
    ) {
    }

    public Summary run(String batchCode, LocalDate startDate, int days, String scale, boolean allowDuplicateBatch) {
        long startedAt = System.currentTimeMillis();
        assertSafeToMutate();
        if (!StringUtils.hasText(batchCode)) {
            throw new IllegalArgumentException("enterprise-demo.batch-code must not be empty");
        }
        if (!allowDuplicateBatch) {
            assertBatchNotAlreadyPresent(batchCode);
        }

        ScaleConfig cfg = ScaleConfig.forScale(scale);
        log.info("enterprise-demo bulk insert starting: batchCode={}, startDate={}, days={}, scale={}", batchCode, startDate, days, scale);

        Sim sim = new Sim(jdbcTemplate, batchCode, cfg);
        sim.insertMasterData();
        for (int d = 0; d < days; d++) {
            LocalDate date = startDate.plusDays(d);
            sim.simulateDay(date);
            if (d % 30 == 0 || d == days - 1) {
                log.info("enterprise-demo progress day {}/{} ({}): inbound={} outbound={} adjust={} count={} movements={} exceptions={} logs={}",
                        d + 1, days, date, sim.inboundOrderCount, sim.outboundOrderCount, sim.adjustOrderCount,
                        sim.countTaskCount, sim.movementCount, sim.exceptionCount, sim.operationLogCount);
            }
        }
        sim.flushAll();
        sim.insertFinalInventory();
        sim.sequenceAllocator.flushToDatabase(jdbcTemplate);

        Summary summary = new Summary(
                batchCode, startDate, days, cfg.name(),
                cfg.warehouseCount(), sim.areaCount, sim.locationCount, cfg.skuCount(), cfg.supplierCount(), cfg.customerCount(),
                sim.inboundOrderCount, sim.outboundOrderCount, sim.adjustOrderCount, sim.countTaskCount,
                sim.ledger.size(), sim.movementCount, sim.exceptionCount, sim.operationLogCount,
                System.currentTimeMillis() - startedAt
        );
        log.info("enterprise-demo bulk insert complete: {}", summary);
        return summary;
    }

    /** Two independent signals, either one refuses the call: an active Spring profile whose name
     * contains "prod", or the connected database's own name containing "prod"/"production"/"online".
     * Checked on every call (not just once at startup) so this stays safe even if invoked directly
     * from a test or some future caller, not only from the {@code ApplicationRunner}. */
    private void assertSafeToMutate() {
        for (String profile : environment.getActiveProfiles()) {
            if (profile.toLowerCase(Locale.ROOT).contains("prod")) {
                throw new IllegalStateException("Refusing to run enterprise-demo bulk insert: active Spring profile '" + profile
                        + "' looks like production. This must only run against a local dev/test database.");
            }
        }
        String databaseName = jdbcTemplate.queryForObject("SELECT DATABASE()", String.class);
        String lowerCaseName = databaseName == null ? "" : databaseName.toLowerCase(Locale.ROOT);
        for (String forbidden : FORBIDDEN_DATABASE_NAME_SUBSTRINGS) {
            if (lowerCaseName.contains(forbidden)) {
                throw new IllegalStateException("Refusing to run enterprise-demo bulk insert: current database '" + databaseName
                        + "' looks like production. This must only run against a local dev/test database.");
            }
        }
    }

    private void assertBatchNotAlreadyPresent(String batchCode) {
        Long skuHits = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM skus WHERE code LIKE ?", Long.class, batchCode + "-SKU-%");
        Long warehouseHits = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM warehouses WHERE code LIKE ?", Long.class, batchCode + "-WH-%");
        Long inboundHits = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM inbound_orders WHERE source_order_no = ?", Long.class, batchCode);
        Long logHits = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_operation_log WHERE content LIKE ?", Long.class, "%" + batchCode + "%");
        long total = nz(skuHits) + nz(warehouseHits) + nz(inboundHits) + nz(logHits);
        if (total > 0) {
            throw new IllegalStateException("batchCode '" + batchCode + "' already has data (skus=" + nz(skuHits)
                    + ", warehouses=" + nz(warehouseHits) + ", inbound_orders=" + nz(inboundHits) + ", operation_logs=" + nz(logHits)
                    + "). Pass --enterprise-demo.allow-duplicate-batch=true to insert anyway, or pick a new batchCode.");
        }
    }

    private static long nz(Long value) {
        return value == null ? 0L : value;
    }

    // ============================================================================================
    // scale configuration
    // ============================================================================================

    record ScaleConfig(
            String name, int warehouseCount, int areasPerWarehouse, int locationsPerArea,
            int skuCount, int supplierCount, int customerCount,
            int inboundPerDay, int outboundPerDay, double adjustPerDayAvg, int countTaskIntervalDays
    ) {
        static ScaleConfig forScale(String scale) {
            if ("large".equalsIgnoreCase(scale)) {
                return new ScaleConfig("large", 5, 6, 70, 1500, 150, 400, 15, 100, 3.5, 5);
            }
            return new ScaleConfig("medium", 5, 6, 50, 800, 120, 300, 10, 50, 2.5, 6);
        }
    }

    // ============================================================================================
    // biz number sequence allocator — mirrors BizNoGeneratorService's format/semantics for bulk use
    // ============================================================================================

    static final class SequenceAllocator {
        private final Map<String, Map<String, Long>> nextValueByTypeAndDate = new HashMap<>();
        private final Map<String, Map<String, Boolean>> rowExistsByTypeAndDate = new HashMap<>();

        SequenceAllocator(JdbcTemplate jdbcTemplate) {
        }

        /** Returns the formatted bizNo (e.g. IN202607090001) and reserves that sequence number. */
        String allocate(JdbcTemplate jdbcTemplate, String bizType, String prefix, String seqDate) {
            Map<String, Long> byDate = nextValueByTypeAndDate.computeIfAbsent(bizType, k -> new HashMap<>());
            Long next = byDate.get(seqDate);
            if (next == null) {
                Long existing = jdbcTemplate.query(
                        "SELECT current_value FROM biz_sequence WHERE biz_type = ? AND seq_date = ?",
                        rs -> rs.next() ? rs.getLong(1) : null, bizType, seqDate);
                boolean rowExists = existing != null;
                rowExistsByTypeAndDate.computeIfAbsent(bizType, k -> new HashMap<>()).put(seqDate, rowExists);
                next = (rowExists ? existing : 0L) + 1;
            } else {
                next = next + 1;
            }
            byDate.put(seqDate, next);

            String seqPart = String.valueOf(next);
            if (seqPart.length() < 4) {
                seqPart = "0".repeat(4 - seqPart.length()) + seqPart;
            }
            return prefix + seqDate + seqPart;
        }

        void flushToDatabase(JdbcTemplate jdbcTemplate) {
            for (Map.Entry<String, Map<String, Long>> typeEntry : nextValueByTypeAndDate.entrySet()) {
                String bizType = typeEntry.getKey();
                for (Map.Entry<String, Long> dateEntry : typeEntry.getValue().entrySet()) {
                    String seqDate = dateEntry.getKey();
                    long finalValue = dateEntry.getValue();
                    boolean rowExisted = Boolean.TRUE.equals(rowExistsByTypeAndDate.getOrDefault(bizType, Map.of()).get(seqDate));
                    Timestamp now = Timestamp.valueOf(LocalDateTime.now());
                    if (rowExisted) {
                        jdbcTemplate.update("UPDATE biz_sequence SET current_value = ?, updated_at = ? WHERE biz_type = ? AND seq_date = ?",
                                finalValue, now, bizType, seqDate);
                    } else {
                        String prefix = switch (bizType) {
                            case "INBOUND_ORDER" -> "IN";
                            case "OUTBOUND_ORDER" -> "OUT";
                            case "STOCK_ADJUST_ORDER" -> "ADJ";
                            case "STOCK_COUNT_TASK" -> "CNT";
                            default -> "X";
                        };
                        jdbcTemplate.update(
                                "INSERT INTO biz_sequence (id, biz_type, seq_date, prefix, current_value, remark, created_at, updated_at) VALUES (?,?,?,?,?,?,?,?)",
                                IdWorker.getId(), bizType, seqDate, prefix, finalValue, "enterprise-demo bulk insert", now, now);
                    }
                }
            }
        }
    }

    // ============================================================================================
    // in-memory inventory ledger — mirrors InventoryService's guarded mutations exactly
    // ============================================================================================

    record LedgerKey(long warehouseId, long skuId, long areaId, long locationId) {
    }

    static final class LedgerEntry {
        long onHand;
        long reserved;
        long frozen;
        final boolean exceptionArea;

        LedgerEntry(boolean exceptionArea) {
            this.exceptionArea = exceptionArea;
        }

        long available() {
            return onHand - reserved - frozen;
        }
    }

    static final class Ledger {
        private final Map<LedgerKey, LedgerEntry> entries = new LinkedHashMap<>();
        /** (warehouseId, skuId, NORMAL-area-only) -> candidate keys, for outbound allocation. */
        private final Map<Long, Map<Long, List<LedgerKey>>> normalCandidatesByWarehouseAndSku = new HashMap<>();

        int size() {
            return entries.size();
        }

        LedgerEntry getOrCreate(LedgerKey key, boolean exceptionArea) {
            LedgerEntry entry = entries.get(key);
            if (entry == null) {
                entry = new LedgerEntry(exceptionArea);
                entries.put(key, entry);
                if (!exceptionArea) {
                    normalCandidatesByWarehouseAndSku
                            .computeIfAbsent(key.warehouseId(), k -> new HashMap<>())
                            .computeIfAbsent(key.skuId(), k -> new ArrayList<>())
                            .add(key);
                }
            }
            return entry;
        }

        LedgerEntry get(LedgerKey key) {
            return entries.get(key);
        }

        /** SKUs that actually have (or ever had) stock at this warehouse — used so outbound demand
         * gets generated against plausible SKUs instead of a uniformly random pick across the whole
         * catalog, which would cause an unrealistic stockout storm any time the catalog is larger
         * than what's been received so far (a "cold start" artifact, not a demand-side data point). */
        List<Long> stockedSkuIds(long warehouseId) {
            return new ArrayList<>(normalCandidatesByWarehouseAndSku.getOrDefault(warehouseId, Map.of()).keySet());
        }

        List<LedgerKey> normalCandidates(long warehouseId, long skuId) {
            return normalCandidatesByWarehouseAndSku
                    .getOrDefault(warehouseId, Map.of())
                    .getOrDefault(skuId, List.of());
        }

        Map<LedgerKey, LedgerEntry> all() {
            return entries;
        }

        void increase(LedgerEntry entry, long qty) {
            entry.onHand += qty;
            if (entry.exceptionArea) {
                entry.frozen += qty;
            }
        }

        /** @return true if applied, false if insufficient (caller must not have mutated anything else yet). */
        boolean decrease(LedgerEntry entry, long qty) {
            if (entry.onHand - qty < entry.reserved + entry.frozen) {
                return false;
            }
            entry.onHand -= qty;
            return true;
        }

        boolean lock(LedgerEntry entry, long qty) {
            if (entry.available() < qty) {
                return false;
            }
            entry.reserved += qty;
            return true;
        }

        void ship(LedgerEntry entry, long qty) {
            entry.onHand -= qty;
            entry.reserved -= qty;
        }

        void releaseLock(LedgerEntry entry, long qty) {
            entry.reserved -= qty;
        }

        boolean decreaseFrozenOnHand(LedgerEntry entry, long qty) {
            if (entry.onHand < qty || entry.frozen < qty) {
                return false;
            }
            entry.onHand -= qty;
            entry.frozen -= qty;
            return true;
        }
    }

    // ============================================================================================
    // table buffer — batches JDBC inserts, flushing at a bounded size
    // ============================================================================================

    static final class TableBuffer {
        private final JdbcTemplate jdbcTemplate;
        private final String sql;
        private final List<Object[]> rows = new ArrayList<>(BATCH_FLUSH_SIZE);

        TableBuffer(JdbcTemplate jdbcTemplate, String sql) {
            this.jdbcTemplate = jdbcTemplate;
            this.sql = sql;
        }

        void add(Object[] row) {
            rows.add(row);
            if (rows.size() >= BATCH_FLUSH_SIZE) {
                flush();
            }
        }

        void flush() {
            if (rows.isEmpty()) {
                return;
            }
            jdbcTemplate.batchUpdate(sql, rows);
            rows.clear();
        }
    }

    // ============================================================================================
    // the simulation itself
    // ============================================================================================

    private static final class Sim {
        final JdbcTemplate jdbcTemplate;
        final String batchCode;
        final ScaleConfig cfg;
        final Random random;
        final SequenceAllocator sequenceAllocator = new SequenceAllocator(null);
        final Ledger ledger = new Ledger();

        final TableBuffer warehouseBuf;
        final TableBuffer areaBuf;
        final TableBuffer locationBuf;
        final TableBuffer skuBuf;
        final TableBuffer supplierBuf;
        final TableBuffer customerBuf;
        final TableBuffer inboundOrderBuf;
        final TableBuffer inboundItemBuf;
        final TableBuffer outboundOrderBuf;
        final TableBuffer outboundItemBuf;
        final TableBuffer outboundLockBuf;
        final TableBuffer movementBuf;
        final TableBuffer adjustOrderBuf;
        final TableBuffer adjustItemBuf;
        final TableBuffer countTaskBuf;
        final TableBuffer countItemBuf;
        final TableBuffer exceptionBuf;
        final TableBuffer operationLogBuf;
        final TableBuffer inventoryBuf;
        final List<TableBuffer> allBuffers = new ArrayList<>();

        record LocationRef(long id, long areaId) {
        }

        final List<Long> warehouseIds = new ArrayList<>();
        final Map<Long, EnumMap<AreaTypeLite, List<LocationRef>>> locationsByWarehouseAndAreaType = new HashMap<>();
        final Map<Long, Long> exceptionAreaIdByWarehouse = new HashMap<>();
        final List<Long> skuIds = new ArrayList<>();
        final List<Long> riskSkuIds = new ArrayList<>();
        final List<Long> qualitySensitiveSkuIds = new ArrayList<>();
        final List<Long> highFreqSkuIds = new ArrayList<>();
        final List<Long> supplierIds = new ArrayList<>();
        final List<Long> riskySupplierIds = new ArrayList<>();
        final List<Long> customerIds = new ArrayList<>();
        final List<Long> highCancelCustomerIds = new ArrayList<>();

        long adminUserId;
        String adminUsername = "admin";

        int areaCount = 0;
        int locationCount = 0;
        int inboundOrderCount = 0;
        int outboundOrderCount = 0;
        int adjustOrderCount = 0;
        int countTaskCount = 0;
        int movementCount = 0;
        int exceptionCount = 0;
        int operationLogCount = 0;

        private double adjustCarry = 0;

        enum AreaTypeLite {RECEIVING, STORAGE, PICKING, EXCEPTION, SHIPPING, QC}

        Sim(JdbcTemplate jdbcTemplate, String batchCode, ScaleConfig cfg) {
            this.jdbcTemplate = jdbcTemplate;
            this.batchCode = batchCode;
            this.cfg = cfg;
            this.random = new Random(batchCode.hashCode());

            warehouseBuf = buf("INSERT INTO warehouses (id, code, name, address, enabled, created_at, updated_at) VALUES (?,?,?,?,?,?,?)");
            areaBuf = buf("INSERT INTO warehouse_areas (id, warehouse_id, area_code, area_name, area_type, status, pick_priority, remark, enabled, created_at, updated_at) VALUES (?,?,?,?,?,?,?,?,?,?,?)");
            locationBuf = buf("INSERT INTO warehouse_locations (id, warehouse_id, area_id, location_code, location_name, location_type, status, capacity_qty, used_qty, allow_mixed_sku, pick_priority, remark, enabled, created_at, updated_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
            skuBuf = buf("INSERT INTO skus (id, code, name, unit, category, enabled, created_at, updated_at) VALUES (?,?,?,?,?,?,?,?)");
            supplierBuf = buf("INSERT INTO suppliers (id, code, name, contact_name, contact_phone, address, enabled, created_at, updated_at) VALUES (?,?,?,?,?,?,?,?,?)");
            customerBuf = buf("INSERT INTO customers (id, code, name, contact_name, contact_phone, address, enabled, created_at, updated_at) VALUES (?,?,?,?,?,?,?,?,?)");
            inboundOrderBuf = buf("INSERT INTO inbound_orders (id, order_no, status, warehouse_id, supplier_id, received_at, enabled, created_at, updated_at, source_type, source_order_no, external_order_no) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)");
            inboundItemBuf = buf("INSERT INTO inbound_order_items (id, order_id, sku_id, quantity, enabled, area_id, location_id) VALUES (?,?,?,?,?,?,?)");
            outboundOrderBuf = buf("INSERT INTO outbound_orders (id, order_no, status, warehouse_id, customer_id, shipped_at, enabled, created_at, updated_at, source_type, source_order_no, external_order_no) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)");
            outboundItemBuf = buf("INSERT INTO outbound_order_items (id, order_id, sku_id, quantity, enabled) VALUES (?,?,?,?,?)");
            outboundLockBuf = buf("INSERT INTO outbound_stock_locks (id, outbound_order_id, outbound_order_item_id, sku_id, warehouse_id, area_id, location_id, lock_qty, shipped_qty, status, created_at, updated_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)");
            movementBuf = buf("INSERT INTO stock_movements (id, type, warehouse_id, sku_id, quantity_change, business_no, remark, occurred_at, enabled, area_id, location_id, before_quantity, after_quantity, before_reserved_quantity, after_reserved_quantity, operator, before_frozen_quantity, after_frozen_quantity, before_available_quantity, after_available_quantity, operation_type) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
            adjustOrderBuf = buf("INSERT INTO stock_adjust_order (id, adjust_no, status, reason_type, reason, warehouse_id, created_by, confirmed_by, confirmed_at, cancelled_by, cancelled_at, cancel_reason, enabled, created_at, updated_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
            adjustItemBuf = buf("INSERT INTO stock_adjust_order_item (id, adjust_order_id, sku_id, warehouse_id, area_id, location_id, adjust_type, adjust_qty, before_on_hand_qty, after_on_hand_qty, before_locked_qty, after_locked_qty, before_frozen_qty, after_frozen_qty, before_available_qty, after_available_qty, remark, enabled, created_at, updated_at, inventory_id, adjust_action, target_warehouse_id, target_area_id, target_location_id, target_inventory_id, hold_qty, hold_status) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
            countTaskBuf = buf("INSERT INTO stock_count_task (id, count_no, warehouse_id, area_id, location_id, status, remark, created_by, completed_by, completed_at, cancelled_by, cancelled_at, cancel_reason, enabled, created_at, updated_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
            countItemBuf = buf("INSERT INTO stock_count_item (id, count_task_id, sku_id, warehouse_id, area_id, location_id, book_on_hand_qty, book_locked_qty, book_frozen_qty, book_available_qty, actual_qty, diff_qty, status, remark, enabled, created_at, updated_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
            exceptionBuf = buf("INSERT INTO wms_exception_events (id, exception_type, biz_no, sku_id, warehouse_id, area_id, location_id, message, status, handler_id, handled_time, create_time) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)");
            operationLogBuf = buf("INSERT INTO sys_operation_log (id, operator, operation_type, biz_no, biz_type, biz_id, content, ip, create_time, user_id, module, request_uri, request_method, success, error_message) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
            inventoryBuf = buf("INSERT INTO inventory (id, warehouse_id, sku_id, quantity, reserved_quantity, enabled, created_at, updated_at, area_id, location_id, frozen_quantity, inventory_status) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)");

            Long fetchedAdminId = jdbcTemplate.query("SELECT id FROM sys_user WHERE username = 'admin' LIMIT 1", rs -> rs.next() ? rs.getLong(1) : null);
            adminUserId = fetchedAdminId != null ? fetchedAdminId : IdWorker.getId();
        }

        private TableBuffer buf(String sql) {
            TableBuffer b = new TableBuffer(jdbcTemplate, sql);
            allBuffers.add(b);
            return b;
        }

        void flushAll() {
            allBuffers.forEach(TableBuffer::flush);
        }

        // ---------------------------------------------------------------------------------------
        // master data
        // ---------------------------------------------------------------------------------------

        private static final String[][] WAREHOUSE_DEFS = {
                {"WH-SH", "上海主仓", "上海市奉贤区物流园区1号"},
                {"WH-SZ", "苏州备货仓", "江苏省苏州市工业园区2号"},
                {"WH-BJ", "北京北方仓", "北京市顺义区物流基地3号"},
                {"WH-GZ", "广州华南仓", "广东省广州市白云区物流中心4号"},
                {"WH-CD", "成都西南仓", "四川省成都市青白江区物流园5号"}
        };

        private static final Object[][] AREA_DEFS = {
                {"REC", "收货区", AreaTypeLite.RECEIVING, 0},
                {"STO", "存储区", AreaTypeLite.STORAGE, 10},
                {"PIC", "拣货区", AreaTypeLite.PICKING, 5},
                {"EXP", "异常区", AreaTypeLite.EXCEPTION, 0},
                {"SHP", "发货区", AreaTypeLite.SHIPPING, 0},
                {"QC", "质检区", AreaTypeLite.QC, 0}
        };

        private static final String[] SKU_CATEGORIES = {
                "电子配件", "包材", "五金件", "耗材", "易损件", "高周转件", "低周转件", "退货高风险件", "质检敏感件"
        };

        void insertMasterData() {
            Timestamp now = Timestamp.valueOf(LocalDateTime.now());

            for (int w = 0; w < cfg.warehouseCount() && w < WAREHOUSE_DEFS.length; w++) {
                String[] def = WAREHOUSE_DEFS[w];
                long warehouseId = IdWorker.getId();
                warehouseIds.add(warehouseId);
                warehouseBuf.add(new Object[]{warehouseId, batchCode + "-" + def[0], def[1], def[2], 1, now, now});

                EnumMap<AreaTypeLite, List<LocationRef>> byType = new EnumMap<>(AreaTypeLite.class);
                locationsByWarehouseAndAreaType.put(warehouseId, byType);

                for (Object[] areaDef : AREA_DEFS) {
                    long areaId = IdWorker.getId();
                    String areaCode = batchCode + "-" + def[0] + "-" + areaDef[0];
                    AreaTypeLite areaType = (AreaTypeLite) areaDef[2];
                    areaBuf.add(new Object[]{areaId, warehouseId, areaCode, areaDef[1], areaType.name(), "ENABLED", areaDef[3], batchCode, 1, now, now});
                    areaCount++;
                    if (areaType == AreaTypeLite.EXCEPTION) {
                        exceptionAreaIdByWarehouse.put(warehouseId, areaId);
                    }

                    List<LocationRef> locations = new ArrayList<>();
                    for (int i = 1; i <= cfg.locationsPerArea(); i++) {
                        long locationId = IdWorker.getId();
                        String locationCode = areaCode + "-A" + String.format("%03d", i);
                        locationBuf.add(new Object[]{locationId, warehouseId, areaId, locationCode, locationCode, "NORMAL", "ENABLED",
                                5000, 0, 1, 1, batchCode, 1, now, now});
                        locationCount++;
                        locations.add(new LocationRef(locationId, areaId));
                    }
                    byType.put(areaType, locations);
                }
            }

            int perCategory = Math.max(1, cfg.skuCount() / SKU_CATEGORIES.length);
            int seq = 1;
            for (int c = 0; c < SKU_CATEGORIES.length && seq <= cfg.skuCount(); c++) {
                String category = SKU_CATEGORIES[c];
                int countForCategory = (c == SKU_CATEGORIES.length - 1) ? (cfg.skuCount() - seq + 1) : perCategory;
                for (int i = 0; i < countForCategory && seq <= cfg.skuCount(); i++, seq++) {
                    long skuId = IdWorker.getId();
                    String code = batchCode + "-SKU-" + String.format("%05d", seq);
                    skuBuf.add(new Object[]{skuId, code, category + "-" + seq, "PCS", category, 1, now, now});
                    skuIds.add(skuId);
                    if ("退货高风险件".equals(category)) {
                        riskSkuIds.add(skuId);
                    } else if ("质检敏感件".equals(category)) {
                        qualitySensitiveSkuIds.add(skuId);
                    } else if ("高周转件".equals(category) && highFreqSkuIds.size() < Math.max(20, cfg.skuCount() / 10)) {
                        highFreqSkuIds.add(skuId);
                    }
                }
            }
            if (highFreqSkuIds.isEmpty()) {
                highFreqSkuIds.addAll(skuIds.subList(0, Math.min(20, skuIds.size())));
            }

            for (int i = 1; i <= cfg.supplierCount(); i++) {
                long supplierId = IdWorker.getId();
                String code = batchCode + "-SUP-" + String.format("%04d", i);
                supplierBuf.add(new Object[]{supplierId, code, "供应商" + i, "对接人" + i, "139" + String.format("%08d", i), "供应商地址" + i, 1, now, now});
                supplierIds.add(supplierId);
                if (i % 8 == 0) {
                    riskySupplierIds.add(supplierId);
                }
            }
            if (riskySupplierIds.isEmpty() && !supplierIds.isEmpty()) {
                riskySupplierIds.add(supplierIds.get(0));
            }

            for (int i = 1; i <= cfg.customerCount(); i++) {
                long customerId = IdWorker.getId();
                String code = batchCode + "-CUST-" + String.format("%05d", i);
                customerBuf.add(new Object[]{customerId, code, "客户" + i, "联系人" + i, "138" + String.format("%08d", i), "客户地址" + i, 1, now, now});
                customerIds.add(customerId);
                if (i % 10 == 0) {
                    highCancelCustomerIds.add(customerId);
                }
            }
            if (highCancelCustomerIds.isEmpty() && !customerIds.isEmpty()) {
                highCancelCustomerIds.add(customerIds.get(0));
            }

            flushAll();
            log.info("enterprise-demo master data inserted: {} warehouses, {} areas, {} locations, {} skus, {} suppliers, {} customers",
                    warehouseIds.size(), areaCount, locationCount, skuIds.size(), supplierIds.size(), customerIds.size());
        }

        // ---------------------------------------------------------------------------------------
        // per-day simulation
        // ---------------------------------------------------------------------------------------

        void simulateDay(LocalDate date) {
            String seqDate = date.format(SEQ_DATE_FORMAT);
            simulateInbound(date, seqDate);
            simulateOutbound(date, seqDate);
            simulateAdjustments(date, seqDate);
            simulateCounts(date, seqDate);
        }

        private Timestamp randomTimeOn(LocalDate date) {
            int hour = 8 + random.nextInt(11);
            int minute = random.nextInt(60);
            int second = random.nextInt(60);
            return Timestamp.valueOf(date.atTime(hour, minute, second));
        }

        private long pickSku(boolean preferHighFreq) {
            if (preferHighFreq && !highFreqSkuIds.isEmpty() && random.nextInt(100) < 40) {
                return highFreqSkuIds.get(random.nextInt(highFreqSkuIds.size()));
            }
            return skuIds.get(random.nextInt(skuIds.size()));
        }

        /** Outbound demand should almost always target SKUs this warehouse actually has some history
         * of stocking (95%) — a real customer order isn't placed against a SKU nobody has ever
         * received — with a small 5% chance of a genuinely unstocked SKU, which is what actually
         * produces the "ordered something we don't carry here yet" exception scenario intentionally,
         * without letting it dominate the failure count. */
        private long pickOutboundSku(List<Long> stockedSkus) {
            if (!stockedSkus.isEmpty() && random.nextInt(100) < 95) {
                return stockedSkus.get(random.nextInt(stockedSkus.size()));
            }
            return pickSku(true);
        }

        private void simulateInbound(LocalDate date, String seqDate) {
            for (int i = 0; i < cfg.inboundPerDay(); i++) {
                long warehouseId = warehouseIds.get(random.nextInt(warehouseIds.size()));
                boolean useRiskySupplier = !riskySupplierIds.isEmpty() && random.nextInt(100) < 15;
                Long supplierId = supplierIds.isEmpty() ? null
                        : (useRiskySupplier ? riskySupplierIds.get(random.nextInt(riskySupplierIds.size())) : supplierIds.get(random.nextInt(supplierIds.size())));

                String orderNo = sequenceAllocator.allocate(jdbcTemplate, "INBOUND_ORDER", "IN", seqDate);
                long orderId = IdWorker.getId();
                boolean received = random.nextInt(100) < 85;
                Timestamp created = randomTimeOn(date);
                Timestamp receivedAt = received ? new Timestamp(created.getTime() + 3600_000L) : null;

                inboundOrderBuf.add(new Object[]{orderId, orderNo, received ? "RECEIVED" : "CREATED", warehouseId, supplierId,
                        receivedAt, 1, created, receivedAt != null ? receivedAt : created, "MANUAL", batchCode, null});
                inboundOrderCount++;

                int itemCount = 1 + random.nextInt(3);
                List<LocationRef> storageLocations = locationsByWarehouseAndAreaType.get(warehouseId).get(AreaTypeLite.STORAGE);
                for (int k = 0; k < itemCount; k++) {
                    long skuId = useRiskySupplier && !qualitySensitiveSkuIds.isEmpty() && random.nextInt(100) < 50
                            ? qualitySensitiveSkuIds.get(random.nextInt(qualitySensitiveSkuIds.size()))
                            : pickSku(true);
                    LocationRef loc = storageLocations.get(random.nextInt(storageLocations.size()));
                    int qty = 20 + random.nextInt(200);

                    long itemId = IdWorker.getId();
                    inboundItemBuf.add(new Object[]{itemId, orderId, skuId, qty, 1, loc.areaId(), loc.id()});

                    if (received) {
                        LedgerKey key = new LedgerKey(warehouseId, skuId, loc.areaId(), loc.id());
                        LedgerEntry entry = ledger.getOrCreate(key, false);
                        long before = entry.onHand;
                        long beforeReserved = entry.reserved;
                        long beforeFrozen = entry.frozen;
                        ledger.increase(entry, qty);
                        writeMovement("INBOUND", "INBOUND_RECEIVE", warehouseId, skuId, loc.areaId(), loc.id(), qty,
                                orderNo, "入库收货 [" + batchCode + "]", receivedAt,
                                before, entry.onHand, beforeReserved, entry.reserved, beforeFrozen, entry.frozen);
                    }
                }

                writeOperationLog("创建入库单", "INBOUND_ORDER", orderNo, orderId, "入库管理",
                        "/inbound-orders", "POST", created);
                if (received) {
                    writeOperationLog("完成入库", "INBOUND_ORDER", orderNo, orderId, "入库管理",
                            "/inbound-orders/" + orderId + "/receive", "POST", receivedAt);
                }
            }
        }

        private record ItemPick(long skuId, int qty) {
        }

        private record Allocation(long areaId, long locationId, long qty, long skuId, long itemId) {
        }

        private void simulateOutbound(LocalDate date, String seqDate) {
            for (int i = 0; i < cfg.outboundPerDay(); i++) {
                long warehouseId = warehouseIds.get(random.nextInt(warehouseIds.size()));
                boolean highCancelCustomer = !highCancelCustomerIds.isEmpty() && random.nextInt(100) < 20;
                long customerId = highCancelCustomer ? highCancelCustomerIds.get(random.nextInt(highCancelCustomerIds.size()))
                        : customerIds.get(random.nextInt(customerIds.size()));

                String orderNo = sequenceAllocator.allocate(jdbcTemplate, "OUTBOUND_ORDER", "OUT", seqDate);
                long orderId = IdWorker.getId();
                Timestamp created = randomTimeOn(date);

                int itemCount = 1 + random.nextInt(4);
                List<ItemPick> items = new ArrayList<>();
                // Deliberately low: this branch always requests far more than any single location
                // holds, so it's a guaranteed lock failure every time it fires — kept rare so it
                // seasons the "low stock" AI-query narrative instead of dominating the exception count.
                boolean useRiskSku = !riskSkuIds.isEmpty() && random.nextInt(100) < 3;
                List<Long> stockedSkus = ledger.stockedSkuIds(warehouseId);
                for (int k = 0; k < itemCount; k++) {
                    long skuId = useRiskSku && k == 0 ? riskSkuIds.get(random.nextInt(riskSkuIds.size())) : pickOutboundSku(stockedSkus);
                    int qty = useRiskSku && k == 0 ? 500 + random.nextInt(500) : 1 + random.nextInt(10);
                    items.add(new ItemPick(skuId, qty));
                }

                List<Long> itemIds = new ArrayList<>();
                for (ItemPick item : items) {
                    long itemId = IdWorker.getId();
                    itemIds.add(itemId);
                    outboundItemBuf.add(new Object[]{itemId, orderId, item.skuId(), item.qty(), 1});
                }
                outboundOrderCount++;
                writeOperationLog("创建出库单", "OUTBOUND_ORDER", orderNo, orderId, "出库管理", "/outbound-orders", "POST", created);

                int roll = random.nextInt(100);
                boolean wantShip = roll < 65;
                boolean wantLockOnly = !wantShip && roll < 75;
                boolean wantCancelAfterLock = !wantShip && !wantLockOnly && roll < 83;
                boolean wantLockFailure = !wantShip && !wantLockOnly && !wantCancelAfterLock && roll < 91;
                // remaining 9%: left untouched at CREATED, no lock ever attempted

                String status = "CREATED";
                Timestamp shippedAt = null;
                Timestamp updated = created;

                if (wantShip || wantLockOnly || wantCancelAfterLock) {
                    List<Allocation> allocations = tryLockItems(warehouseId, items, itemIds, orderNo, created);
                    if (allocations == null) {
                        recordLockFailureException(warehouseId, items, orderNo, created);
                    } else {
                        Timestamp lockedAt = new Timestamp(created.getTime() + 1800_000L);
                        writeOperationLog("出库锁库", "OUTBOUND_ORDER", orderNo, orderId, "出库管理", "/outbound-orders/" + orderId + "/lock", "POST", lockedAt);
                        updated = lockedAt;

                        if (wantShip) {
                            status = "SHIPPED";
                            shippedAt = new Timestamp(lockedAt.getTime() + 3600_000L);
                            finalizeAllocations(orderId, warehouseId, allocations, orderNo, shippedAt, "SHIPPED");
                            writeOperationLog("出库发货", "OUTBOUND_ORDER", orderNo, orderId, "出库管理", "/outbound-orders/" + orderId + "/ship", "POST", shippedAt);
                            updated = shippedAt;
                        } else if (wantCancelAfterLock) {
                            status = "CANCELLED";
                            Timestamp cancelledAt = new Timestamp(lockedAt.getTime() + 1800_000L);
                            finalizeAllocations(orderId, warehouseId, allocations, orderNo, cancelledAt, "RELEASED");
                            writeOperationLog("出库取消", "OUTBOUND_ORDER", orderNo, orderId, "出库管理", "/outbound-orders/" + orderId + "/cancel", "POST", cancelledAt);
                            updated = cancelledAt;
                        } else {
                            status = "LOCKED";
                            insertLockRows(orderId, warehouseId, allocations, "LOCKED", lockedAt);
                        }
                    }
                } else if (wantLockFailure) {
                    recordLockFailureException(warehouseId, items, orderNo, created);
                }

                outboundOrderBuf.add(new Object[]{orderId, orderNo, status, warehouseId, customerId, shippedAt, 1, created, updated, "MANUAL", batchCode, null});
            }
        }

        /** Verifies every item can be fully allocated and, if so, reserves it in the ledger and writes the
         * OUTBOUND_LOCK movements — but does not insert {@code outbound_stock_locks} rows yet, since the
         * eventual row status (LOCKED/SHIPPED/RELEASED) depends on what the caller decides to do next.
         * Returns null (ledger left untouched) if any item can't be fully allocated.
         * <p>
         * The planning pass tracks a local "provisional" take per ledger key instead of mutating the
         * ledger immediately — two items in the same order can resolve to the same (sku, warehouse)
         * candidate, and checking {@code entry.available()} fresh for the second item without
         * accounting for what the first item already claimed would double-allocate the same stock. */
        private List<Allocation> tryLockItems(long warehouseId, List<ItemPick> items, List<Long> itemIds, String orderNo, Timestamp when) {
            List<Allocation> allocations = new ArrayList<>();
            Map<LedgerKey, Long> provisionalTake = new HashMap<>();

            for (int idx = 0; idx < items.size(); idx++) {
                ItemPick item = items.get(idx);
                long itemId = itemIds.get(idx);
                List<LedgerKey> candidates = ledger.normalCandidates(warehouseId, item.skuId());
                int remaining = item.qty();
                for (LedgerKey key : candidates) {
                    if (remaining <= 0) {
                        break;
                    }
                    LedgerEntry entry = ledger.get(key);
                    long alreadyPlanned = provisionalTake.getOrDefault(key, 0L);
                    long take = Math.min(entry.available() - alreadyPlanned, remaining);
                    if (take <= 0) {
                        continue;
                    }
                    allocations.add(new Allocation(key.areaId(), key.locationId(), take, item.skuId(), itemId));
                    provisionalTake.merge(key, take, Long::sum);
                    remaining -= take;
                }
                if (remaining > 0) {
                    return null;
                }
            }

            for (Allocation a : allocations) {
                LedgerKey key = new LedgerKey(warehouseId, a.skuId(), a.areaId(), a.locationId());
                LedgerEntry entry = ledger.get(key);
                long beforeOnHand = entry.onHand, beforeReserved = entry.reserved, beforeFrozen = entry.frozen;
                ledger.lock(entry, a.qty());
                writeMovement("LOCK", "OUTBOUND_LOCK", warehouseId, a.skuId(), a.areaId(), a.locationId(), 0,
                        orderNo, "出库锁库 [" + batchCode + "]", when,
                        beforeOnHand, entry.onHand, beforeReserved, entry.reserved, beforeFrozen, entry.frozen);
            }
            return allocations;
        }

        private void insertLockRows(long orderId, long warehouseId, List<Allocation> allocations, String status, Timestamp when) {
            for (Allocation a : allocations) {
                long lockId = IdWorker.getId();
                int shippedQty = "SHIPPED".equals(status) ? (int) a.qty() : 0;
                outboundLockBuf.add(new Object[]{lockId, orderId, a.itemId(), a.skuId(), warehouseId, a.areaId(), a.locationId(), a.qty(), shippedQty, status, when, when});
            }
        }

        private void finalizeAllocations(long orderId, long warehouseId, List<Allocation> allocations, String orderNo, Timestamp when, String finalLockStatus) {
            for (Allocation a : allocations) {
                LedgerKey key = new LedgerKey(warehouseId, a.skuId(), a.areaId(), a.locationId());
                LedgerEntry entry = ledger.get(key);
                long beforeOnHand = entry.onHand, beforeReserved = entry.reserved, beforeFrozen = entry.frozen;
                if ("SHIPPED".equals(finalLockStatus)) {
                    ledger.ship(entry, a.qty());
                    writeMovement("OUTBOUND", "OUTBOUND_SHIP", warehouseId, a.skuId(), a.areaId(), a.locationId(), (int) -a.qty(),
                            orderNo, "出库发货 [" + batchCode + "]", when, beforeOnHand, entry.onHand, beforeReserved, entry.reserved, beforeFrozen, entry.frozen);
                } else {
                    ledger.releaseLock(entry, a.qty());
                    writeMovement("UNLOCK", "OUTBOUND_CANCEL_UNLOCK", warehouseId, a.skuId(), a.areaId(), a.locationId(), 0,
                            orderNo, "出库取消解锁 [" + batchCode + "]", when, beforeOnHand, entry.onHand, beforeReserved, entry.reserved, beforeFrozen, entry.frozen);
                }
                long lockId = IdWorker.getId();
                int shippedQty = "SHIPPED".equals(finalLockStatus) ? (int) a.qty() : 0;
                outboundLockBuf.add(new Object[]{lockId, orderId, a.itemId(), a.skuId(), warehouseId, a.areaId(), a.locationId(), a.qty(), shippedQty, finalLockStatus, when, when});
            }
        }

        private void recordLockFailureException(long warehouseId, List<ItemPick> items, String orderNo, Timestamp when) {
            long skuId = items.get(0).skuId();
            long id = IdWorker.getId();
            exceptionBuf.add(new Object[]{id, "INVENTORY_NOT_ENOUGH", orderNo, skuId, warehouseId, null, null,
                    "available inventory is insufficient for outbound order " + orderNo + " [" + batchCode + "]",
                    "OPEN", null, null, when});
            exceptionCount++;
        }

        // ---------------------------------------------------------------------------------------
        // stock adjust orders
        // ---------------------------------------------------------------------------------------

        private void simulateAdjustments(LocalDate date, String seqDate) {
            adjustCarry += cfg.adjustPerDayAvg();
            int todayCount = (int) adjustCarry;
            adjustCarry -= todayCount;

            for (int i = 0; i < todayCount; i++) {
                long warehouseId = warehouseIds.get(random.nextInt(warehouseIds.size()));
                Timestamp created = randomTimeOn(date);
                String adjustNo = sequenceAllocator.allocate(jdbcTemplate, "STOCK_ADJUST_ORDER", "ADJ", seqDate);
                long orderId = IdWorker.getId();

                int actionRoll = random.nextInt(100);
                boolean leaveIncomplete = random.nextInt(100) < 5;

                if (actionRoll < 35) {
                    simulateQuantityAdjust(warehouseId, adjustNo, orderId, created, true, leaveIncomplete);
                } else if (actionRoll < 70) {
                    simulateQuantityAdjust(warehouseId, adjustNo, orderId, created, false, leaveIncomplete);
                } else if (actionRoll < 90) {
                    simulateTransferToException(warehouseId, adjustNo, orderId, created, leaveIncomplete);
                } else {
                    if (!simulateRestoreFromException(warehouseId, adjustNo, orderId, created, leaveIncomplete)) {
                        simulateQuantityAdjust(warehouseId, adjustNo, orderId, created, true, leaveIncomplete);
                    }
                }
                adjustOrderCount++;
            }
        }

        private LedgerKey pickExistingNormalEntry(long warehouseId) {
            for (int attempt = 0; attempt < 20; attempt++) {
                long skuId = pickSku(false);
                List<LedgerKey> candidates = ledger.normalCandidates(warehouseId, skuId);
                if (!candidates.isEmpty()) {
                    return candidates.get(random.nextInt(candidates.size()));
                }
            }
            return null;
        }

        private void simulateQuantityAdjust(long warehouseId, String adjustNo, long orderId, Timestamp created, boolean increase, boolean leaveIncomplete) {
            LedgerKey key = pickExistingNormalEntry(warehouseId);
            if (key == null) {
                return;
            }
            LedgerEntry entry = ledger.get(key);
            String reasonType = increase ? (random.nextBoolean() ? "FOUND" : "DATA_ERROR") : (random.nextBoolean() ? "LOST" : "DATA_ERROR");
            int qty = increase ? 1 + random.nextInt(20) : (int) Math.min(entry.available(), 1 + random.nextInt(10));
            if (!increase && qty <= 0) {
                return;
            }

            String status = leaveIncomplete ? pickIncompleteStatus() : "COMPLETED";
            long itemId = IdWorker.getId();
            long beforeOnHand = entry.onHand, beforeReserved = entry.reserved, beforeFrozen = entry.frozen;

            if ("COMPLETED".equals(status)) {
                if (increase) {
                    ledger.increase(entry, qty);
                } else if (!ledger.decrease(entry, qty)) {
                    return;
                }
                writeMovement("ADJUSTMENT", increase ? "STOCK_ADJUST_INCREASE" : "STOCK_ADJUST_DECREASE",
                        warehouseId, key.skuId(), key.areaId(), key.locationId(), increase ? qty : -qty,
                        adjustNo, (increase ? "库存调增" : "库存调减") + " [" + batchCode + "]", created,
                        beforeOnHand, entry.onHand, beforeReserved, entry.reserved, beforeFrozen, entry.frozen);
            }

            insertAdjustOrderAndItem(orderId, adjustNo, status, reasonType, warehouseId, created,
                    itemId, key.skuId(), key.areaId(), key.locationId(), increase ? "INCREASE" : "DECREASE",
                    increase ? "QUANTITY_INCREASE" : "QUANTITY_DECREASE", increase ? qty : -qty,
                    beforeOnHand, "COMPLETED".equals(status) ? entry.onHand : beforeOnHand,
                    beforeReserved, beforeFrozen, null, null, null, null, 0, "NONE", null);

            logAdjustOperations(adjustNo, orderId, status, created);
        }

        private void simulateTransferToException(long warehouseId, String adjustNo, long orderId, Timestamp created, boolean leaveIncomplete) {
            Long exceptionAreaId = exceptionAreaIdByWarehouse.get(warehouseId);
            if (exceptionAreaId == null) {
                return;
            }
            LedgerKey sourceKey = pickExistingNormalEntry(warehouseId);
            if (sourceKey == null) {
                return;
            }
            LedgerEntry source = ledger.get(sourceKey);
            int qty = (int) Math.min(source.available(), 1 + random.nextInt(10));
            if (qty <= 0) {
                return;
            }
            String reasonType = random.nextBoolean() ? "DAMAGE" : "QUALITY_ISSUE";
            List<LocationRef> exceptionLocations = locationsByWarehouseAndAreaType.get(warehouseId).get(AreaTypeLite.EXCEPTION);
            LocationRef targetLoc = exceptionLocations.get(random.nextInt(exceptionLocations.size()));
            LedgerKey targetKey = new LedgerKey(warehouseId, sourceKey.skuId(), targetLoc.areaId(), targetLoc.id());
            LedgerEntry target = ledger.getOrCreate(targetKey, true);

            String status = leaveIncomplete ? pickIncompleteStatus() : "COMPLETED";
            long itemId = IdWorker.getId();
            long srcBeforeOnHand = source.onHand, srcBeforeReserved = source.reserved, srcBeforeFrozen = source.frozen;
            long tgtBeforeOnHand = target.onHand, tgtBeforeFrozen = target.frozen;
            String holdStatus = "NONE";

            if ("COMPLETED".equals(status)) {
                if (!ledger.decreaseFrozenOnHand(source, qty)) {
                    // Real semantics require the source to already carry `qty` frozen going into confirm
                    // (submit's hold step). Our net-effect simulation skips modeling the intermediate HELD
                    // state, so a plain "decrease onHand only" is the correct final effect here.
                    if (!ledger.decrease(source, qty)) {
                        return;
                    }
                }
                ledger.increase(target, qty);
                writeMovement("ADJUSTMENT", "TRANSFER_TO_EXCEPTION_OUT", warehouseId, sourceKey.skuId(), sourceKey.areaId(), sourceKey.locationId(), -qty,
                        adjustNo, "转异常区-转出 [" + batchCode + "]", created,
                        srcBeforeOnHand, source.onHand, srcBeforeReserved, source.reserved, srcBeforeFrozen, source.frozen);
                writeMovement("ADJUSTMENT", "TRANSFER_TO_EXCEPTION_IN", warehouseId, sourceKey.skuId(), targetKey.areaId(), targetKey.locationId(), qty,
                        adjustNo, "转异常区-转入 [" + batchCode + "]", created,
                        tgtBeforeOnHand, target.onHand, 0, 0, tgtBeforeFrozen, target.frozen);
                holdStatus = "CONSUMED";
            }

            insertAdjustOrderAndItem(orderId, adjustNo, status, reasonType, warehouseId, created,
                    itemId, sourceKey.skuId(), sourceKey.areaId(), sourceKey.locationId(), "DECREASE", "TRANSFER_TO_EXCEPTION", -qty,
                    srcBeforeOnHand, "COMPLETED".equals(status) ? source.onHand : srcBeforeOnHand,
                    srcBeforeReserved, srcBeforeFrozen,
                    warehouseId, targetKey.areaId(), targetKey.locationId(), qty, "CONSUMED".equals(holdStatus) ? qty : 0, holdStatus, null);

            logAdjustOperations(adjustNo, orderId, status, created);
        }

        private boolean simulateRestoreFromException(long warehouseId, String adjustNo, long orderId, Timestamp created, boolean leaveIncomplete) {
            List<LocationRef> exceptionLocations = locationsByWarehouseAndAreaType.get(warehouseId).get(AreaTypeLite.EXCEPTION);
            LedgerKey sourceKey = null;
            for (LocationRef loc : exceptionLocations) {
                for (long skuId : skuIds) {
                    LedgerKey candidate = new LedgerKey(warehouseId, skuId, loc.areaId(), loc.id());
                    LedgerEntry entry = ledger.get(candidate);
                    if (entry != null && entry.frozen > 0) {
                        sourceKey = candidate;
                        break;
                    }
                }
                if (sourceKey != null) {
                    break;
                }
            }
            if (sourceKey == null) {
                return false;
            }
            LedgerEntry source = ledger.get(sourceKey);
            int qty = (int) Math.min(source.frozen, 1 + random.nextInt(5));
            if (qty <= 0) {
                return false;
            }
            List<LocationRef> storageLocations = locationsByWarehouseAndAreaType.get(warehouseId).get(AreaTypeLite.STORAGE);
            LocationRef targetLoc = storageLocations.get(random.nextInt(storageLocations.size()));
            LedgerKey targetKey = new LedgerKey(warehouseId, sourceKey.skuId(), targetLoc.areaId(), targetLoc.id());
            LedgerEntry target = ledger.getOrCreate(targetKey, false);

            String status = leaveIncomplete ? pickIncompleteStatus() : "COMPLETED";
            long itemId = IdWorker.getId();
            long srcBeforeOnHand = source.onHand, srcBeforeFrozen = source.frozen;
            long tgtBeforeOnHand = target.onHand;

            if ("COMPLETED".equals(status)) {
                if (!ledger.decreaseFrozenOnHand(source, qty)) {
                    return false;
                }
                ledger.increase(target, qty);
                writeMovement("ADJUSTMENT", "RESTORE_FROM_EXCEPTION_OUT", warehouseId, sourceKey.skuId(), sourceKey.areaId(), sourceKey.locationId(), -qty,
                        adjustNo, "异常恢复-转出 [" + batchCode + "]", created,
                        srcBeforeOnHand, source.onHand, 0, 0, srcBeforeFrozen, source.frozen);
                writeMovement("ADJUSTMENT", "RESTORE_FROM_EXCEPTION_IN", warehouseId, sourceKey.skuId(), targetKey.areaId(), targetKey.locationId(), qty,
                        adjustNo, "异常恢复-转入 [" + batchCode + "]", created,
                        tgtBeforeOnHand, target.onHand, 0, 0, 0, target.frozen);
            }

            insertAdjustOrderAndItem(orderId, adjustNo, status, "OTHER", warehouseId, created,
                    itemId, sourceKey.skuId(), sourceKey.areaId(), sourceKey.locationId(), "INCREASE", "RESTORE_FROM_EXCEPTION", qty,
                    srcBeforeOnHand, "COMPLETED".equals(status) ? source.onHand : srcBeforeOnHand,
                    0, srcBeforeFrozen, warehouseId, targetKey.areaId(), targetKey.locationId(), qty, 0, "NONE", null);

            logAdjustOperations(adjustNo, orderId, status, created);
            return true;
        }

        private String pickIncompleteStatus() {
            return switch (random.nextInt(3)) {
                case 0 -> "DRAFT";
                case 1 -> "SUBMITTED";
                default -> "CANCELLED";
            };
        }

        private void insertAdjustOrderAndItem(long orderId, String adjustNo, String status, String reasonType, long warehouseId, Timestamp created,
                long itemId, long skuId, long areaId, long locationId, String adjustType, String adjustAction, int adjustQty,
                long beforeOnHand, long afterOnHand, long beforeReserved, long beforeFrozen,
                Long targetWarehouseId, Long targetAreaId, Long targetLocationId, Integer holdQty, int holdQtyFinal, String holdStatus, Long targetInventoryId) {

            boolean completed = "COMPLETED".equals(status);
            boolean cancelled = "CANCELLED".equals(status);
            String createdBy = adminUsername;
            String confirmedBy = completed ? adminUsername : null;
            Timestamp confirmedAt = completed ? new Timestamp(created.getTime() + 1800_000L) : null;
            String cancelledBy = cancelled ? adminUsername : null;
            Timestamp cancelledAt = cancelled ? new Timestamp(created.getTime() + 1800_000L) : null;
            String cancelReason = cancelled ? ("批量演示取消 [" + batchCode + "]") : null;
            Timestamp updated = confirmedAt != null ? confirmedAt : (cancelledAt != null ? cancelledAt : created);

            adjustOrderBuf.add(new Object[]{orderId, adjustNo, status, reasonType, "批量演示数据 [" + batchCode + "]", warehouseId,
                    createdBy, confirmedBy, confirmedAt, cancelledBy, cancelledAt, cancelReason, 1, created, updated});

            adjustItemBuf.add(new Object[]{itemId, orderId, skuId, warehouseId, areaId, locationId, adjustType, adjustQty,
                    beforeOnHand, afterOnHand, beforeReserved, beforeReserved, beforeFrozen, beforeFrozen,
                    beforeOnHand - beforeReserved - beforeFrozen, afterOnHand - beforeReserved - beforeFrozen,
                    "批量演示数据 [" + batchCode + "]", 1, created, updated,
                    null, adjustAction, targetWarehouseId, targetAreaId, targetLocationId, targetInventoryId, holdQtyFinal, holdStatus});
        }

        private void logAdjustOperations(String adjustNo, long orderId, String status, Timestamp created) {
            writeOperationLog("创建库存调整单", "STOCK_ADJUST_ORDER", adjustNo, orderId, "库存调整", "/stock-adjust-orders", "POST", created);
            if (!"DRAFT".equals(status)) {
                writeOperationLog("提交库存调整单", "STOCK_ADJUST_ORDER", adjustNo, orderId, "库存调整", "/stock-adjust-orders/" + orderId + "/submit", "POST", created);
            }
            if ("COMPLETED".equals(status)) {
                writeOperationLog("确认库存调整单", "STOCK_ADJUST_ORDER", adjustNo, orderId, "库存调整", "/stock-adjust-orders/" + orderId + "/confirm", "POST", created);
            } else if ("CANCELLED".equals(status)) {
                writeOperationLog("取消库存调整单", "STOCK_ADJUST_ORDER", adjustNo, orderId, "库存调整", "/stock-adjust-orders/" + orderId + "/cancel", "POST", created);
            }
        }

        // ---------------------------------------------------------------------------------------
        // stock count tasks
        // ---------------------------------------------------------------------------------------

        private int dayIndexCounter = 0;

        private void simulateCounts(LocalDate date, String seqDate) {
            dayIndexCounter++;
            if (dayIndexCounter % cfg.countTaskIntervalDays() != 0) {
                return;
            }
            long warehouseId = warehouseIds.get(random.nextInt(warehouseIds.size()));
            List<LocationRef> storageLocations = locationsByWarehouseAndAreaType.get(warehouseId).get(AreaTypeLite.STORAGE);
            LocationRef loc = storageLocations.get(random.nextInt(storageLocations.size()));

            String countNo = sequenceAllocator.allocate(jdbcTemplate, "STOCK_COUNT_TASK", "CNT", seqDate);
            long taskId = IdWorker.getId();
            Timestamp created = randomTimeOn(date);
            boolean cancelled = random.nextInt(100) < 5;

            List<LedgerKey> itemsAtLocation = new ArrayList<>();
            for (long skuId : skuIds) {
                LedgerKey key = new LedgerKey(warehouseId, skuId, loc.areaId(), loc.id());
                if (ledger.get(key) != null) {
                    itemsAtLocation.add(key);
                }
            }

            String status = itemsAtLocation.isEmpty() || cancelled ? "CANCELLED" : "COMPLETED";
            Timestamp completedAt = "COMPLETED".equals(status) ? new Timestamp(created.getTime() + 7200_000L) : null;
            Timestamp cancelledAt = "CANCELLED".equals(status) ? new Timestamp(created.getTime() + 1800_000L) : null;
            Timestamp updated = completedAt != null ? completedAt : (cancelledAt != null ? cancelledAt : created);

            countTaskBuf.add(new Object[]{taskId, countNo, warehouseId, loc.areaId(), loc.id(), status, "周期盘点 [" + batchCode + "]",
                    adminUsername, "COMPLETED".equals(status) ? adminUsername : null, completedAt,
                    "CANCELLED".equals(status) ? adminUsername : null, cancelledAt,
                    "CANCELLED".equals(status) ? ("无库存或抽样取消 [" + batchCode + "]") : null, 1, created, updated});
            countTaskCount++;

            if ("COMPLETED".equals(status)) {
                for (LedgerKey key : itemsAtLocation) {
                    LedgerEntry entry = ledger.get(key);
                    long bookOnHand = entry.onHand, bookReserved = entry.reserved, bookFrozen = entry.frozen;
                    int outcomeRoll = random.nextInt(100);
                    long itemId = IdWorker.getId();
                    long actual = bookOnHand;
                    Integer diff = 0;

                    boolean canDecrease = bookReserved == 0 && bookFrozen == 0;
                    if (outcomeRoll < 20) {
                        long delta = Math.max(1, bookOnHand / 20);
                        actual = bookOnHand + delta;
                        diff = (int) delta;
                        ledger.increase(entry, delta);
                        writeMovement("COUNT", "STOCK_COUNT_PROFIT", warehouseId, key.skuId(), key.areaId(), key.locationId(), (int) delta,
                                countNo, "盘点盘盈 [" + batchCode + "]", completedAt,
                                bookOnHand, entry.onHand, bookReserved, entry.reserved, bookFrozen, entry.frozen);
                    } else if (outcomeRoll < 40 && canDecrease && bookOnHand > 0) {
                        long delta = Math.min(bookOnHand, Math.max(1, bookOnHand / 20));
                        if (ledger.decrease(entry, delta)) {
                            actual = bookOnHand - delta;
                            diff = (int) -delta;
                            writeMovement("COUNT", "STOCK_COUNT_LOSS", warehouseId, key.skuId(), key.areaId(), key.locationId(), (int) -delta,
                                    countNo, "盘点盘亏 [" + batchCode + "]", completedAt,
                                    bookOnHand, entry.onHand, bookReserved, entry.reserved, bookFrozen, entry.frozen);
                        }
                    }

                    countItemBuf.add(new Object[]{itemId, taskId, key.skuId(), warehouseId, key.areaId(), key.locationId(),
                            bookOnHand, bookReserved, bookFrozen, bookOnHand - bookReserved - bookFrozen,
                            actual, diff, "RECORDED", "批量演示数据 [" + batchCode + "]", 1, created, updated});
                }
            }

            writeOperationLog("创建盘点任务", "STOCK_COUNT_TASK", countNo, taskId, "库存盘点", "/stock-count-tasks", "POST", created);
            if ("COMPLETED".equals(status)) {
                writeOperationLog("开始库存盘点", "STOCK_COUNT_TASK", countNo, taskId, "库存盘点", "/stock-count-tasks/" + taskId + "/start", "POST", created);
                writeOperationLog("完成库存盘点", "STOCK_COUNT_TASK", countNo, taskId, "库存盘点", "/stock-count-tasks/" + taskId + "/complete", "POST", completedAt);
            } else {
                writeOperationLog("取消库存盘点", "STOCK_COUNT_TASK", countNo, taskId, "库存盘点", "/stock-count-tasks/" + taskId + "/cancel", "POST", cancelledAt != null ? cancelledAt : created);
            }
        }

        // ---------------------------------------------------------------------------------------
        // shared helpers
        // ---------------------------------------------------------------------------------------

        private void writeMovement(String type, String operationType, long warehouseId, long skuId, long areaId, long locationId, int quantityChange,
                String businessNo, String remark, Timestamp occurredAt,
                long beforeOnHand, long afterOnHand, long beforeReserved, long afterReserved, long beforeFrozen, long afterFrozen) {
            long id = IdWorker.getId();
            long beforeAvailable = beforeOnHand - beforeReserved - beforeFrozen;
            long afterAvailable = afterOnHand - afterReserved - afterFrozen;
            movementBuf.add(new Object[]{id, type, warehouseId, skuId, quantityChange, businessNo, remark, occurredAt, 1,
                    areaId, locationId, beforeOnHand, afterOnHand, beforeReserved, afterReserved, adminUsername,
                    beforeFrozen, afterFrozen, beforeAvailable, afterAvailable, operationType});
            movementCount++;
        }

        private void writeOperationLog(String operationType, String bizType, String bizNo, long bizId, String module,
                String requestUri, String requestMethod, Timestamp when) {
            long id = IdWorker.getId();
            operationLogBuf.add(new Object[]{id, adminUsername, operationType, bizNo, bizType, bizId,
                    operationType + " [" + batchCode + "]", "127.0.0.1", when, adminUserId, module, requestUri, requestMethod, 1, null});
            operationLogCount++;
        }

        // ---------------------------------------------------------------------------------------
        // final inventory snapshot
        // ---------------------------------------------------------------------------------------

        void insertFinalInventory() {
            Timestamp now = Timestamp.valueOf(LocalDateTime.now());
            for (Map.Entry<LedgerKey, LedgerEntry> e : ledger.all().entrySet()) {
                LedgerKey key = e.getKey();
                LedgerEntry entry = e.getValue();
                long id = IdWorker.getId();
                inventoryBuf.add(new Object[]{id, key.warehouseId(), key.skuId(), entry.onHand, entry.reserved, 1, now, now,
                        key.areaId(), key.locationId(), entry.frozen, entry.exceptionArea ? "EXCEPTION" : "NORMAL"});
            }
            inventoryBuf.flush();
            log.info("enterprise-demo final inventory rows: {}", ledger.size());
        }
    }
}
