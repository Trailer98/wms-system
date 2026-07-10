package com.example.wms.admin.aidemo;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Post-insert consistency validator for {@link EnterpriseYearDemoDataBulkInserter}. Every check is a
 * single SQL query that must return zero (or, where noted, must confirm a table's absence); nothing
 * here mutates data. Runs automatically at the end of {@link EnterpriseYearDemoDataRunner}, and can
 * also be called directly (e.g. from a test) with just a batchCode.
 * <p>
 * Every business-data check is scoped to the batch under test (via a {@code warehouse_id IN (...
 * code LIKE 'batchCode-WH-%')} filter) rather than scanning the whole table. This tool is explicitly
 * forbidden from ever deleting data (including its own previous runs' rows, or anything left behind
 * by an earlier buggy run during development), so an unscoped check would keep failing forever on
 * old data long after the bug that produced it was fixed — the check needs to answer "is *this*
 * batch consistent", not "has this database ever contained a bad row". The only checks that stay
 * global on purpose are structural, batch-independent invariants (orphaned child rows, biz_sequence
 * not falling behind the max issued number) that are safe to evaluate database-wide.
 */
@Component
public class EnterpriseYearDemoDataIntegrityChecker {

    private final JdbcTemplate jdbcTemplate;

    public EnterpriseYearDemoDataIntegrityChecker(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public record CheckResult(String name, boolean passed, String detail) {
    }

    public record Report(List<CheckResult> results) {
        public boolean allPassed() {
            return results.stream().allMatch(CheckResult::passed);
        }

        public long failedCount() {
            return results.stream().filter(r -> !r.passed()).count();
        }
    }

    public Report check(String batchCode) {
        List<CheckResult> results = new ArrayList<>();
        String whPattern = batchCode + "-WH-%";

        results.add(countCheck("no_negative_inventory",
                "SELECT COUNT(*) FROM inventory WHERE (quantity < 0 OR reserved_quantity < 0 OR frozen_quantity < 0) "
                        + "AND " + warehouseFilter(), whPattern));

        results.add(countCheck("on_hand_covers_reserved_plus_frozen",
                "SELECT COUNT(*) FROM inventory WHERE quantity < reserved_quantity + frozen_quantity AND " + warehouseFilter(), whPattern));

        results.add(countCheck("available_never_negative",
                "SELECT COUNT(*) FROM inventory WHERE (quantity - reserved_quantity - frozen_quantity) < 0 AND " + warehouseFilter(), whPattern));

        results.add(countCheck("inventory_dimension_no_duplicates",
                "SELECT COUNT(*) FROM (SELECT warehouse_id, sku_id, area_id, location_id, COUNT(*) c FROM inventory "
                        + "WHERE " + warehouseFilter() + " GROUP BY warehouse_id, sku_id, area_id, location_id HAVING c > 1) dup", whPattern));

        results.add(countCheck("movements_reference_existing_sku_warehouse",
                "SELECT COUNT(*) FROM stock_movements sm WHERE sm." + warehouseFilter() + " AND ("
                        + "NOT EXISTS (SELECT 1 FROM skus s WHERE s.id = sm.sku_id) "
                        + "OR NOT EXISTS (SELECT 1 FROM warehouses w WHERE w.id = sm.warehouse_id))", whPattern));

        results.add(countCheck("movements_reference_existing_location",
                "SELECT COUNT(*) FROM stock_movements sm WHERE sm." + warehouseFilter() + " AND sm.location_id IS NOT NULL "
                        + "AND NOT EXISTS (SELECT 1 FROM warehouse_locations l WHERE l.id = sm.location_id)", whPattern));

        // Orphan-child checks are structural, batch-independent invariants — safe to leave unscoped.
        results.add(countCheck("inbound_items_have_parent_order",
                "SELECT COUNT(*) FROM inbound_order_items i WHERE NOT EXISTS (SELECT 1 FROM inbound_orders o WHERE o.id = i.order_id)"));

        results.add(countCheck("outbound_items_have_parent_order",
                "SELECT COUNT(*) FROM outbound_order_items i WHERE NOT EXISTS (SELECT 1 FROM outbound_orders o WHERE o.id = i.order_id)"));

        results.add(countCheck("outbound_locks_have_parent_order",
                "SELECT COUNT(*) FROM outbound_stock_locks l WHERE NOT EXISTS (SELECT 1 FROM outbound_orders o WHERE o.id = l.outbound_order_id)"));

        results.add(countCheck("adjust_items_have_parent_order",
                "SELECT COUNT(*) FROM stock_adjust_order_item i WHERE NOT EXISTS (SELECT 1 FROM stock_adjust_order o WHERE o.id = i.adjust_order_id)"));

        results.add(countCheck("count_items_have_parent_task",
                "SELECT COUNT(*) FROM stock_count_item i WHERE NOT EXISTS (SELECT 1 FROM stock_count_task t WHERE t.id = i.count_task_id)"));

        results.add(countCheck("received_inbound_orders_have_movement",
                "SELECT COUNT(*) FROM inbound_orders io WHERE io." + warehouseFilter() + " AND io.status = 'RECEIVED' AND NOT EXISTS ("
                        + "SELECT 1 FROM stock_movements sm WHERE sm.business_no = io.order_no AND sm.operation_type = 'INBOUND_RECEIVE')", whPattern));

        results.add(countCheck("shipped_outbound_orders_have_movement",
                "SELECT COUNT(*) FROM outbound_orders oo WHERE oo." + warehouseFilter() + " AND oo.status = 'SHIPPED' AND NOT EXISTS ("
                        + "SELECT 1 FROM stock_movements sm WHERE sm.business_no = oo.order_no AND sm.operation_type = 'OUTBOUND_SHIP')", whPattern));

        results.add(countCheck("shipped_orders_never_left_negative_inventory",
                "SELECT COUNT(*) FROM stock_movements WHERE " + warehouseFilter() + " AND operation_type = 'OUTBOUND_SHIP' AND after_quantity < 0", whPattern));

        results.add(countCheck("transfer_to_exception_net_on_hand_unchanged",
                "SELECT COUNT(*) FROM ("
                        + "  SELECT business_no, SUM(quantity_change) net FROM stock_movements"
                        + "  WHERE " + warehouseFilter() + " AND operation_type IN ('TRANSFER_TO_EXCEPTION_OUT', 'TRANSFER_TO_EXCEPTION_IN')"
                        + "  GROUP BY business_no HAVING net <> 0"
                        + ") bad", whPattern));

        results.add(countCheck("restore_from_exception_net_on_hand_unchanged",
                "SELECT COUNT(*) FROM ("
                        + "  SELECT business_no, SUM(quantity_change) net FROM stock_movements"
                        + "  WHERE " + warehouseFilter() + " AND operation_type IN ('RESTORE_FROM_EXCEPTION_OUT', 'RESTORE_FROM_EXCEPTION_IN')"
                        + "  GROUP BY business_no HAVING net <> 0"
                        + ") bad", whPattern));

        results.add(noFinanceEventTableCheck());

        results.add(countCheck("completed_adjust_quantity_items_have_movement",
                "SELECT COUNT(*) FROM stock_adjust_order_item i JOIN stock_adjust_order o ON o.id = i.adjust_order_id "
                        + "WHERE o." + warehouseFilter() + " AND o.status = 'COMPLETED' AND i.adjust_action IN ('QUANTITY_INCREASE', 'QUANTITY_DECREASE') AND NOT EXISTS ("
                        + "  SELECT 1 FROM stock_movements sm WHERE sm.business_no = o.adjust_no AND sm.operation_type = "
                        + "  CASE i.adjust_action WHEN 'QUANTITY_INCREASE' THEN 'STOCK_ADJUST_INCREASE' ELSE 'STOCK_ADJUST_DECREASE' END)", whPattern));

        results.add(countCheck("profit_loss_count_items_have_movement",
                "SELECT COUNT(*) FROM stock_count_item i JOIN stock_count_task t ON t.id = i.count_task_id "
                        + "WHERE t." + warehouseFilter() + " AND t.status = 'COMPLETED' AND i.diff_qty IS NOT NULL AND i.diff_qty <> 0 AND NOT EXISTS ("
                        + "  SELECT 1 FROM stock_movements sm WHERE sm.business_no = t.count_no AND sm.operation_type = "
                        + "  CASE WHEN i.diff_qty > 0 THEN 'STOCK_COUNT_PROFIT' ELSE 'STOCK_COUNT_LOSS' END)", whPattern));

        results.add(countCheck("creation_operation_logs_have_biz_no",
                "SELECT COUNT(*) FROM sys_operation_log WHERE content LIKE CONCAT('%', ?, '%') "
                        + "AND operation_type LIKE '创建%' AND success = 1 AND (biz_no IS NULL OR biz_no = '')", batchCode));

        results.add(batchCodeMarkerCheck(batchCode));

        // biz_sequence is a genuinely global, batch-independent table (one row per real calendar
        // date regardless of which batch minted numbers on it), so this deliberately checks the
        // system-wide max issued number, not just this batch's.
        results.add(bizSequenceNotBehindCheck("INBOUND_ORDER", "order_no", "inbound_orders"));
        results.add(bizSequenceNotBehindCheck("OUTBOUND_ORDER", "order_no", "outbound_orders"));
        results.add(bizSequenceNotBehindCheck("STOCK_ADJUST_ORDER", "adjust_no", "stock_adjust_order"));
        results.add(bizSequenceNotBehindCheck("STOCK_COUNT_TASK", "count_no", "stock_count_task"));

        return new Report(results);
    }

    private static String warehouseFilter() {
        return "warehouse_id IN (SELECT id FROM warehouses WHERE code LIKE ?)";
    }

    private CheckResult countCheck(String name, String sql, Object... args) {
        Long count = jdbcTemplate.queryForObject(sql, Long.class, args);
        long value = count == null ? 0 : count;
        return new CheckResult(name, value == 0, value == 0 ? "0 violations" : value + " violation(s) found");
    }

    /** There is no finance-event table/service in this codebase (only aspirational comments in
     * StockAdjustOrderService), so this passes vacuously — kept as an explicit named check so it
     * starts failing loudly the day a finance-event table is introduced without matching logic here. */
    private CheckResult noFinanceEventTableCheck() {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name LIKE '%finance_event%'",
                Long.class);
        long value = count == null ? 0 : count;
        return new CheckResult("no_finance_event_table_yet", value == 0,
                value == 0 ? "confirmed absent (finance events intentionally skipped)" : "a finance-event table now exists; this checker needs updating");
    }

    private CheckResult batchCodeMarkerCheck(String batchCode) {
        Long skuHits = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM skus WHERE code LIKE ?", Long.class, batchCode + "-SKU-%");
        Long warehouseHits = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM warehouses WHERE code LIKE ?", Long.class, batchCode + "-WH-%");
        long total = (skuHits == null ? 0 : skuHits) + (warehouseHits == null ? 0 : warehouseHits);
        return new CheckResult("batch_code_identifiable", total > 0,
                total > 0 ? total + " rows carry the batchCode marker" : "no rows found carrying batchCode '" + batchCode + "'");
    }

    private CheckResult bizSequenceNotBehindCheck(String bizType, String noColumn, String table) {
        String prefix = switch (bizType) {
            case "INBOUND_ORDER" -> "IN";
            case "OUTBOUND_ORDER" -> "OUT";
            case "STOCK_ADJUST_ORDER" -> "ADJ";
            default -> "CNT";
        };
        String maxNoSql = "SELECT MAX(" + noColumn + ") FROM " + table + " WHERE " + noColumn + " LIKE ?";
        String maxNo = jdbcTemplate.query(maxNoSql, rs -> rs.next() ? rs.getString(1) : null, prefix + "%");
        if (maxNo == null) {
            return new CheckResult("biz_sequence_not_behind_" + bizType, true, "no orders of this type generated, nothing to check");
        }
        String seqDate = maxNo.substring(prefix.length(), prefix.length() + 8);
        long maxSeq = Long.parseLong(maxNo.substring(prefix.length() + 8));
        Long currentValue = jdbcTemplate.query(
                "SELECT current_value FROM biz_sequence WHERE biz_type = ? AND seq_date = ?",
                rs -> rs.next() ? rs.getLong(1) : null, bizType, seqDate);
        boolean ok = currentValue != null && currentValue >= maxSeq;
        return new CheckResult("biz_sequence_not_behind_" + bizType, ok,
                ok ? "biz_sequence.current_value=" + currentValue + " >= max issued seq=" + maxSeq
                        : "biz_sequence.current_value=" + currentValue + " is BEHIND max issued seq=" + maxSeq + " for date " + seqDate);
    }
}
