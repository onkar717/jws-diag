package org.jboss.jws.diag.logs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.jws.diag.logs.formatter.LogsJsonFormatter;
import org.jboss.jws.diag.logs.model.LogPattern;
import org.jboss.jws.diag.logs.model.LogScanResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LogsJsonFormatterTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @TempDir
    Path tempDir;

    @Test
    void outputHasSchemaVersionAndFileAndLinesScanned() throws IOException {
        Path log = writeLog("INFO server started");
        LogScanResult result = new LogScanner().scan(log);
        JsonNode root = parse(new LogsJsonFormatter().format(result));

        assertThat(root.get("schemaVersion").asText()).isEqualTo("1.0");
        assertThat(root.get("file").asText()).isEqualTo(log.toString());
        assertThat(root.get("linesScanned").asLong()).isEqualTo(1);
    }

    @Test
    void allPatternsAppearInOutput() throws IOException {
        Path log = writeLog("INFO server started");
        LogScanResult result = new LogScanner().scan(log);
        JsonNode patterns = parse(new LogsJsonFormatter().format(result)).get("patterns");

        assertThat(patterns.size()).isEqualTo(LogPattern.values().length);
        for (LogPattern p : LogPattern.values()) {
            boolean found = false;
            for (JsonNode node : patterns) {
                if (p.getId().equals(node.get("id").asText())) {
                    found = true;
                    break;
                }
            }
            assertThat(found).as("pattern %s present", p.getId()).isTrue();
        }
    }

    @Test
    void detectedOomPatternHasCorrectCountAndMatch() throws IOException {
        Path log = writeLog(
                "INFO regular line",
                "java.lang.OutOfMemoryError: Java heap space"
        );
        LogScanResult result = new LogScanner().scan(log);
        JsonNode patterns = parse(new LogsJsonFormatter().format(result)).get("patterns");

        JsonNode oomNode = findPattern(patterns, "OOM");
        assertThat(oomNode.get("count").asInt()).isEqualTo(1);
        assertThat(oomNode.get("severity").asText()).isEqualTo("ERROR");

        JsonNode match = oomNode.get("matches").get(0);
        assertThat(match.get("line").asLong()).isEqualTo(2);
        assertThat(match.get("text").asText()).contains("OutOfMemoryError");
    }

    @Test
    void zeroCountPatternHasEmptyMatchesArray() throws IOException {
        Path log = writeLog("INFO server started");
        LogScanResult result = new LogScanner().scan(log);
        JsonNode patterns = parse(new LogsJsonFormatter().format(result)).get("patterns");

        JsonNode oomNode = findPattern(patterns, "OOM");
        assertThat(oomNode.get("count").asInt()).isZero();
        assertThat(oomNode.get("matches").size()).isZero();
    }

    @Test
    void gcOverheadOomLineCreditedToOomOnly() throws IOException {
        Path log = writeLog(
                "java.lang.OutOfMemoryError: GC overhead limit exceeded"
        );
        LogScanResult result = new LogScanner().scan(log);
        JsonNode patterns = parse(new LogsJsonFormatter().format(result)).get("patterns");

        assertThat(findPattern(patterns, "OOM").get("count").asInt()).isEqualTo(1);
        assertThat(findPattern(patterns, "GC_OVERHEAD").get("count").asInt()).isZero();
    }

    private Path writeLog(String... lines) throws IOException {
        Path log = tempDir.resolve("catalina.out");
        Files.write(log, List.of(lines));
        return log;
    }

    private JsonNode parse(String json) throws IOException {
        return MAPPER.readTree(json);
    }

    private JsonNode findPattern(JsonNode patterns, String id) {
        for (JsonNode node : patterns) {
            if (id.equals(node.get("id").asText())) return node;
        }
        throw new AssertionError("Pattern not found: " + id);
    }
}
