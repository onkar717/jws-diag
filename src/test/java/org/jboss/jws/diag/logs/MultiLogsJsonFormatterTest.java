package org.jboss.jws.diag.logs;

import org.jboss.jws.diag.logs.formatter.MultiLogsJsonFormatter;
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

class MultiLogsJsonFormatterTest {

    private static final Path LOG = Path.of("/opt/jws-6.0/standalone/logs/catalina.out");

    private final MultiLogsJsonFormatter formatter = new MultiLogsJsonFormatter();

    @Test
    void output_containsSchemaVersion() {
        String json = formatter.format(emptyReport());

        assertThat(json).contains("\"schemaVersion\"");
        assertThat(json).contains("\"1.0\"");
    }

    @Test
    void output_containsInstanceCount() {
        MultiLogReport report = new MultiLogReport(3, List.of());

        String json = formatter.format(report);

        assertThat(json).contains("\"instanceCount\" : 3");
    }

    @Test
    void output_containsInstancesArray() {
        InstanceLogResult r = result(1234, LOG);
        MultiLogReport report = new MultiLogReport(1, List.of(r));

        String json = formatter.format(report);

        assertThat(json).contains("\"instances\"");
        assertThat(json).contains("\"pid\" : 1234");
    }

    @Test
    void pathsInJson_useForwardSlashes() {
        InstanceLogResult r = result(1234, LOG);
        MultiLogReport report = new MultiLogReport(1, List.of(r));

        String json = formatter.format(report);

        assertThat(json).doesNotContain("\\\\");
    }

    @Test
    void emptyInstances_rendersValidJson() {
        String json = formatter.format(emptyReport());

        assertThat(json).contains("\"instances\" : [ ]");
    }

    private MultiLogReport emptyReport() {
        return new MultiLogReport(0, List.of());
    }

    private InstanceLogResult result(int pid, Path logPath) {
        Map<LogPattern, Integer> counts = new EnumMap<>(LogPattern.class);
        Map<LogPattern, List<org.jboss.jws.diag.logs.model.LogMatch>> matches = new EnumMap<>(LogPattern.class);
        LogScanResult scanResult = new LogScanResult(logPath, 500, counts, matches);
        return new InstanceLogResult(pid, logPath, scanResult);
    }
}
