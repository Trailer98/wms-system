package com.example.wms.admin.aidemo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Opt-in enterprise-year bulk-insert trigger. Only runs under {@code dev}/{@code test} profiles (the
 * {@code @Profile} means this bean doesn't exist at all otherwise) and only when
 * {@code --enterprise-demo.insert=true} is explicitly passed — every other property has a sane
 * default, but this one deliberately does not, so a normal startup never inserts anything.
 * <p>
 * Deliberately separate parameter namespace ({@code enterprise-demo.*}) from the older
 * {@code ai-demo.*} flags: this tool does not reset, does not go through Services/Controllers, and
 * inserts orders of magnitude more data, so conflating the two flag families would be misleading.
 * <p>
 * Example:
 * {@code java -jar wms-admin.jar --spring.profiles.active=dev --enterprise-demo.insert=true
 * --enterprise-demo.batch-code=ENT_YEAR_2026 --enterprise-demo.start-date=2025-07-01
 * --enterprise-demo.days=365 --enterprise-demo.scale=medium}
 */
@Component
@Profile({"dev", "test"})
public class EnterpriseYearDemoDataRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(EnterpriseYearDemoDataRunner.class);

    private final EnterpriseYearDemoDataBulkInserter bulkInserter;
    private final EnterpriseYearDemoDataIntegrityChecker integrityChecker;

    @Value("${enterprise-demo.insert:false}")
    private boolean insertRequested;

    @Value("${enterprise-demo.batch-code:}")
    private String batchCode;

    @Value("${enterprise-demo.start-date:}")
    private String startDateText;

    @Value("${enterprise-demo.days:365}")
    private int days;

    @Value("${enterprise-demo.scale:medium}")
    private String scale;

    @Value("${enterprise-demo.allow-duplicate-batch:false}")
    private boolean allowDuplicateBatch;

    public EnterpriseYearDemoDataRunner(EnterpriseYearDemoDataBulkInserter bulkInserter, EnterpriseYearDemoDataIntegrityChecker integrityChecker) {
        this.bulkInserter = bulkInserter;
        this.integrityChecker = integrityChecker;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!insertRequested) {
            return;
        }

        if (batchCode == null || batchCode.isBlank()) {
            log.error("enterprise-demo.insert=true requires --enterprise-demo.batch-code=<code>; aborting, nothing was inserted.");
            return;
        }
        LocalDate startDate;
        try {
            startDate = startDateText == null || startDateText.isBlank() ? LocalDate.now().minusDays(days) : LocalDate.parse(startDateText);
        } catch (RuntimeException ex) {
            log.error("enterprise-demo.start-date='{}' is not a valid ISO date (yyyy-MM-dd); aborting.", startDateText);
            return;
        }

        log.info("enterprise-demo.insert=true: starting enterprise-year bulk insert (batchCode={}, startDate={}, days={}, scale={}, allowDuplicateBatch={})",
                batchCode, startDate, days, scale, allowDuplicateBatch);

        EnterpriseYearDemoDataBulkInserter.Summary summary = bulkInserter.run(batchCode, startDate, days, scale, allowDuplicateBatch);
        log.info("enterprise-demo bulk insert finished: {}", summary);

        EnterpriseYearDemoDataIntegrityChecker.Report report = integrityChecker.check(batchCode);
        if (report.allPassed()) {
            log.info("enterprise-demo integrity check: ALL {} checks passed", report.results().size());
        } else {
            log.error("enterprise-demo integrity check FAILED ({} of {} checks failed) — see details below; the inserted data is NOT verified consistent:",
                    report.failedCount(), report.results().size());
            report.results().stream().filter(r -> !r.passed()).forEach(r ->
                    log.error("  FAILED [{}]: {}", r.name(), r.detail()));
        }

        log.info("enterprise-demo: this application will keep running as a normal server; stop it yourself (Ctrl+C) when done inspecting the data.");
    }
}
