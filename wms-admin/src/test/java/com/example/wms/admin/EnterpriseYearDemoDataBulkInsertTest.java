package com.example.wms.admin;

import com.example.wms.admin.aidemo.EnterpriseYearDemoDataBulkInserter;
import com.example.wms.admin.aidemo.EnterpriseYearDemoDataIntegrityChecker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs the enterprise-year bulk inserter at a deliberately small {@code days} value (fast enough for
 * the regular test suite) and asserts every {@link EnterpriseYearDemoDataIntegrityChecker} check
 * passes. The full 365-day/medium-scale run is a separate manual verification step (see the task's
 * final report) — this test exists to catch a broken ledger/SQL-column mismatch cheaply, not to
 * prove the production-scale volumes.
 */
@SpringBootTest(properties = "spring.datasource.url=jdbc:mysql://localhost:3306/wms_system_test?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Tokyo&useSSL=false&allowPublicKeyRetrieval=true")
class EnterpriseYearDemoDataBulkInsertTest {

    @Autowired
    private EnterpriseYearDemoDataBulkInserter bulkInserter;
    @Autowired
    private EnterpriseYearDemoDataIntegrityChecker integrityChecker;

    @Test
    void smallScaleRunIsConsistentAndIdempotentAgainstDuplicateBatchCode() {
        String batchCode = "ENT_TEST_" + System.nanoTime();
        LocalDate startDate = LocalDate.of(2025, 1, 1);

        EnterpriseYearDemoDataBulkInserter.Summary summary = bulkInserter.run(batchCode, startDate, 14, "medium", false);
        assertEquals(5, summary.warehouses());
        assertTrue(summary.inboundOrders() > 0, "expected some inbound orders");
        assertTrue(summary.outboundOrders() > 0, "expected some outbound orders");
        assertTrue(summary.stockMovements() > 0, "expected some stock movements");
        assertTrue(summary.operationLogs() > 0, "expected some operation logs");

        EnterpriseYearDemoDataIntegrityChecker.Report report = integrityChecker.check(batchCode);
        report.results().forEach(r -> assertTrue(r.passed(), "check failed: " + r.name() + " -> " + r.detail()));

        // Re-running the same batchCode without the override flag must refuse, not silently duplicate.
        assertThrows(IllegalStateException.class, () -> bulkInserter.run(batchCode, startDate, 14, "medium", false));
    }

    @Test
    void rejectsEmptyBatchCode() {
        assertThrows(IllegalArgumentException.class, () -> bulkInserter.run("", LocalDate.now(), 5, "medium", false));
        assertThrows(IllegalArgumentException.class, () -> bulkInserter.run(null, LocalDate.now(), 5, "medium", false));
    }
}
