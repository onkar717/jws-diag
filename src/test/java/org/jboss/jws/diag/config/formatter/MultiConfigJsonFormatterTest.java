package org.jboss.jws.diag.config.formatter;

import org.jboss.jws.diag.config.model.InstanceConfigResult;
import org.jboss.jws.diag.config.model.MultiConfigReport;
import org.jboss.jws.diag.config.model.ServerConfig;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MultiConfigJsonFormatterTest {

    private static final Path BASE = Path.of("/opt/jws-6.0/standalone");

    private final MultiConfigJsonFormatter formatter = new MultiConfigJsonFormatter();

    @Test
    void output_containsSchemaVersion() {
        String json = formatter.format(emptyReport());

        assertThat(json).contains("\"schemaVersion\"");
        assertThat(json).contains("\"1.0\"");
    }

    @Test
    void output_containsInstanceCount() {
        MultiConfigReport report = new MultiConfigReport(2, List.of());

        String json = formatter.format(report);

        assertThat(json).contains("\"instanceCount\" : 2");
    }

    @Test
    void output_containsInstancesArray() {
        InstanceConfigResult result = new InstanceConfigResult(1234, BASE, ServerConfig.builder().build());
        MultiConfigReport report = new MultiConfigReport(1, List.of(result));

        String json = formatter.format(report);

        assertThat(json).contains("\"instances\"");
        assertThat(json).contains("\"pid\" : 1234");
    }

    @Test
    void pathsInJson_useForwardSlashes() {
        InstanceConfigResult result = new InstanceConfigResult(1234, BASE, ServerConfig.builder().build());
        MultiConfigReport report = new MultiConfigReport(1, List.of(result));

        String json = formatter.format(report);

        assertThat(json).doesNotContain("\\\\");
    }

    @Test
    void emptyInstances_rendersValidJson() {
        String json = formatter.format(emptyReport());

        assertThat(json).contains("\"instances\" : [ ]");
    }

    private MultiConfigReport emptyReport() {
        return new MultiConfigReport(0, List.of());
    }
}
