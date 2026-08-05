package org.jboss.jws.diag.logs;

import org.jboss.jws.diag.logs.formatter.LogsHumanFormatter;
import org.jboss.jws.diag.logs.model.LogMatch;
import org.jboss.jws.diag.logs.model.LogPattern;
import org.jboss.jws.diag.logs.model.LogScanResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LogsHumanFormatterTest {

    @TempDir
    Path tempDir;

    @Test
    void zeroCountPatterns_showNoSeverityTag() throws IOException {
        Path log = writeCleanLog();
        LogScanResult result = new LogScanner().scan(log);
        String output = new LogsHumanFormatter().format(result);

        assertThat(output).contains("OutOfMemoryError");
        assertThat(output).doesNotContain("[ERROR]");
        assertThat(output).doesNotContain("[WARN]");
    }

    @Test
    void oomMatch_showsErrorTagAndLineNumber() throws IOException {
        Path log = tempDir.resolve("catalina.out");
        Files.write(log, List.of("java.lang.OutOfMemoryError: Java heap space"));
        LogScanResult result = new LogScanner().scan(log);
        String output = new LogsHumanFormatter().format(result);

        assertThat(output).contains("[ERROR]");
        assertThat(output).contains("line      1");
        assertThat(output).contains("OutOfMemoryError");
    }

    @Test
    void excessMatches_showsEllipsisWithCount() throws IOException {
        Path log = tempDir.resolve("catalina.out");
        List<String> lines = new java.util.ArrayList<>();
        for (int i = 0; i < 8; i++) {
            lines.add("java.lang.OutOfMemoryError: heap (occ " + i + ")");
        }
        Files.write(log, lines);
        LogScanResult result = new LogScanner().scan(log);
        String output = new LogsHumanFormatter().format(result);

        assertThat(output).contains("... and 3 more");
    }

    @Test
    void outputContainsHeaderWithFilePath() throws IOException {
        Path log = writeCleanLog();
        LogScanResult result = new LogScanner().scan(log);
        String output = new LogsHumanFormatter().format(result);

        assertThat(output).startsWith("Log Scan: ");
        assertThat(output).contains(log.toString());
        assertThat(output).contains("Lines scanned:");
    }

    @Test
    void allPatternsListed() throws IOException {
        Path log = writeCleanLog();
        LogScanResult result = new LogScanner().scan(log);
        String output = new LogsHumanFormatter().format(result);

        for (LogPattern p : LogPattern.values()) {
            assertThat(output).contains(p.getLabel());
        }
    }

    private Path writeCleanLog() throws IOException {
        Path log = tempDir.resolve("catalina.out");
        Files.writeString(log, "05-Aug-2026 10:00:00.000 INFO [main] Server startup in 1234 ms\n");
        return log;
    }
}
