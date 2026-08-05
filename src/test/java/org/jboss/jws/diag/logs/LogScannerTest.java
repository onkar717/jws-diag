package org.jboss.jws.diag.logs;

import org.jboss.jws.diag.logs.model.LogPattern;
import org.jboss.jws.diag.logs.model.LogScanResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LogScannerTest {

    @TempDir
    Path tempDir;

    @Test
    void scanEmptyFile_returnsZeroCounts() throws IOException {
        Path log = tempDir.resolve("catalina.out");
        Files.writeString(log, "");

        LogScanResult result = new LogScanner().scan(log);

        assertThat(result.getLinesScanned()).isZero();
        for (LogPattern p : LogPattern.values()) {
            assertThat(result.countFor(p)).as("count for %s", p).isZero();
        }
    }

    @Test
    void detectsOutOfMemoryError() throws IOException {
        Path log = writeLog(
                "05-Aug-2026 10:00:00.000 SEVERE [main]",
                "java.lang.OutOfMemoryError: Java heap space",
                "    at java.util.Arrays.copyOf(Arrays.java:3210)"
        );

        LogScanResult result = new LogScanner().scan(log);

        assertThat(result.countFor(LogPattern.OOM)).isEqualTo(1);
        assertThat(result.matchesFor(LogPattern.OOM)).hasSize(1);
        assertThat(result.matchesFor(LogPattern.OOM).get(0).getText())
                .contains("OutOfMemoryError");
        assertThat(result.countFor(LogPattern.BIND_EXCEPTION)).isZero();
    }

    @Test
    void detectsBindException() throws IOException {
        Path log = writeLog(
                "05-Aug-2026 10:00:00.000 SEVERE [main] org.apache.catalina.core.StandardServer.await",
                "java.net.BindException: Address already in use",
                "    at sun.nio.ch.Net.bind0(Native Method)"
        );

        LogScanResult result = new LogScanner().scan(log);

        assertThat(result.countFor(LogPattern.BIND_EXCEPTION)).isEqualTo(1);
        assertThat(result.countFor(LogPattern.OOM)).isZero();
    }

    @Test
    void detectsStuckThread() throws IOException {
        Path log = writeLog(
                "05-Aug-2026 10:01:00.000 SEVERE [StuckThread] " +
                "org.apache.catalina.valves.StuckThreadDetectionValve notifyStuckThreadDetected",
                "Thread http-nio-8080-exec-1 has been stuck for 601 seconds",
                "at java.lang.Object.wait(Native Method)"
        );

        LogScanResult result = new LogScanner().scan(log);

        assertThat(result.countFor(LogPattern.STUCK_THREAD)).isGreaterThanOrEqualTo(1);
    }

    @Test
    void gcOverheadOomCountsAsOomOnly() throws IOException {
        // A line containing both OutOfMemoryError and GC overhead should credit OOM only —
        // GC_OVERHEAD is a subtype of OOM and must not inflate counts separately.
        Path log = writeLog(
                "Exception in thread \"main\" java.lang.OutOfMemoryError: GC overhead limit exceeded",
                "    at com.example.App.main(App.java:10)"
        );

        LogScanResult result = new LogScanner().scan(log);

        assertThat(result.countFor(LogPattern.OOM)).isEqualTo(1);
        assertThat(result.countFor(LogPattern.GC_OVERHEAD)).isZero();
    }

    @Test
    void detectsStandaloneGcOverhead() throws IOException {
        // A standalone GC overhead warning (not wrapped in OutOfMemoryError) counts as GC_OVERHEAD.
        Path log = writeLog(
                "WARNING: GC overhead limit exceeded — consider increasing heap",
                "java.lang.OutOfMemoryError: Java heap space",
                "    at java.util.Arrays.copyOf(Arrays.java:3210)"
        );

        LogScanResult result = new LogScanner().scan(log);

        assertThat(result.countFor(LogPattern.GC_OVERHEAD)).isEqualTo(1);
        assertThat(result.countFor(LogPattern.OOM)).isEqualTo(1);
    }

    @Test
    void detectsClassNotFoundException() throws IOException {
        Path log = writeLog(
                "05-Aug-2026 10:00:00.000 SEVERE [main]",
                "java.lang.ClassNotFoundException: com.mysql.jdbc.Driver",
                "    at java.net.URLClassLoader.findClass(URLClassLoader.java:387)"
        );

        LogScanResult result = new LogScanner().scan(log);

        assertThat(result.countFor(LogPattern.CLASS_NOT_FOUND)).isEqualTo(1);
    }

    @Test
    void capsMatchesAtFiveButCountIsExact() throws IOException {
        List<String> lines = new java.util.ArrayList<>();
        for (int i = 0; i < 10; i++) {
            lines.add("java.lang.OutOfMemoryError: Java heap space (occurrence " + i + ")");
        }
        Path log = tempDir.resolve("catalina.out");
        Files.write(log, lines);

        LogScanResult result = new LogScanner().scan(log);

        assertThat(result.countFor(LogPattern.OOM)).isEqualTo(10);
        assertThat(result.matchesFor(LogPattern.OOM))
                .hasSize(LogScanner.MAX_MATCHES_PER_PATTERN);
    }

    @Test
    void multiplePatternsSameFile() throws IOException {
        Path log = writeLog(
                "java.lang.OutOfMemoryError: Java heap space",
                "java.net.BindException: Address already in use",
                "java.lang.ClassNotFoundException: org.example.Foo"
        );

        LogScanResult result = new LogScanner().scan(log);

        assertThat(result.countFor(LogPattern.OOM)).isEqualTo(1);
        assertThat(result.countFor(LogPattern.BIND_EXCEPTION)).isEqualTo(1);
        assertThat(result.countFor(LogPattern.CLASS_NOT_FOUND)).isEqualTo(1);
        assertThat(result.countFor(LogPattern.STUCK_THREAD)).isZero();
    }

    @Test
    void nonExistentFile_throwsIOException() {
        Path missing = tempDir.resolve("no-such-file.log");
        assertThatThrownBy(() -> new LogScanner().scan(missing))
                .isInstanceOf(IOException.class);
    }

    @Test
    void linesScannedMatchesLineCount() throws IOException {
        Path log = writeLog("line one", "line two", "line three");
        LogScanResult result = new LogScanner().scan(log);
        assertThat(result.getLinesScanned()).isEqualTo(3);
    }

    private Path writeLog(String... lines) throws IOException {
        Path log = tempDir.resolve("catalina.out");
        Files.write(log, List.of(lines));
        return log;
    }
}
