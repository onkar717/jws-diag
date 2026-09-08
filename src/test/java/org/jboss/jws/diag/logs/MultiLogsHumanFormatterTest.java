package org.jboss.jws.diag.logs;

import org.jboss.jws.diag.logs.formatter.MultiLogsHumanFormatter;
import org.jboss.jws.diag.logs.model.InstanceLogResult;
import org.jboss.jws.diag.logs.model.LogPattern;
import org.jboss.jws.diag.logs.model.LogScanResult;
import org.jboss.jws.diag.logs.model.MultiLogReport;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MultiLogsHumanFormatterTest {

    private static final Path LOG_A = Path.of("/opt/jws-6.0/standalone/logs/catalina.out");
    private static final Path LOG_B = Path.of("/opt/jws-5.7/standalone/logs/catalina.out");

    private final MultiLogsHumanFormatter formatter = new MultiLogsHumanFormatter();

    @Test
    void header_showsInstanceCount() {
        MultiLogReport report = reportWith(List.of(
                result(1234, LOG_A),
                result(5678, LOG_B)
        ));

        String out = formatter.format(report);

        assertThat(out).contains("Auto-discovered 2 instance(s).");
    }

    @Test
    void eachInstance_showsPidAndLogPath() {
        MultiLogReport report = reportWith(List.of(
                result(1234, LOG_A),
                result(5678, LOG_B)
        ));

        String out = formatter.format(report);

        assertThat(out).contains("Instance PID 1234");
        assertThat(out).contains(LOG_A.toString().replace('\\', '/'));
        assertThat(out).contains("Instance PID 5678");
        assertThat(out).contains(LOG_B.toString().replace('\\', '/'));
    }

    @Test
    void eachInstance_containsLogScanHeader() {
        MultiLogReport report = reportWith(List.of(result(1234, LOG_A)));

        String out = formatter.format(report);

        assertThat(out).contains("Log Scan:");
        assertThat(out).contains("Lines scanned:");
    }

    @Test
    void singleInstance_oneRuleLine() {
        MultiLogReport report = reportWith(List.of(result(1234, LOG_A)));

        String out = formatter.format(report);

        long ruleCount = out.lines()
                .filter(l -> l.startsWith("─"))
                .count();
        assertThat(ruleCount).isGreaterThanOrEqualTo(1);
    }

    @Test
    void multipleInstances_eachSeparated() {
        MultiLogReport report = reportWith(List.of(
                result(1234, LOG_A),
                result(5678, LOG_B)
        ));

        String out = formatter.format(report);

        // At least 2 rule lines (one per instance header)
        long ruleCount = out.lines()
                .filter(l -> l.contains("─".repeat(10)))
                .count();
        assertThat(ruleCount).isGreaterThanOrEqualTo(2);
    }

    private MultiLogReport reportWith(List<InstanceLogResult> results) {
        return new MultiLogReport(results.size(), results);
    }

    private InstanceLogResult result(int pid, Path logPath) {
        Map<LogPattern, Integer> counts = new EnumMap<>(LogPattern.class);
        Map<LogPattern, List<org.jboss.jws.diag.logs.model.LogMatch>> matches = new EnumMap<>(LogPattern.class);
        LogScanResult scanResult = new LogScanResult(logPath, 500, counts, matches);
        return new InstanceLogResult(pid, logPath, scanResult);
    }
}
