package org.jboss.jws.diag.summary.formatter;

import org.jboss.jws.diag.summary.model.JwsInstallation;
import org.jboss.jws.diag.summary.model.MultiSummaryReport;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MultiSummaryJsonFormatterTest {

    private static final Path BASE = Path.of("/opt/jws-6.0/standalone");

    private final MultiSummaryJsonFormatter formatter = new MultiSummaryJsonFormatter();

    @Test
    void output_containsSchemaVersion() {
        String json = formatter.format(emptyReport());

        assertThat(json).contains("\"schemaVersion\"");
        assertThat(json).contains("\"1.0\"");
    }

    @Test
    void output_containsInstanceCount() {
        MultiSummaryReport report = new MultiSummaryReport(3, List.of());

        String json = formatter.format(report);

        assertThat(json).contains("\"instanceCount\" : 3");
    }

    @Test
    void output_containsInstancesArray() {
        JwsInstallation inst = JwsInstallation.builder()
                .pid(1234)
                .catalinaBase(BASE)
                .tomcatVersion("10.1.49")
                .build();
        MultiSummaryReport report = new MultiSummaryReport(1, List.of(inst));

        String json = formatter.format(report);

        assertThat(json).contains("\"instances\"");
        assertThat(json).contains("1234");
        assertThat(json).contains("10.1.49");
    }

    @Test
    void emptyInstances_rendersValidJson() {
        String json = formatter.format(emptyReport());

        assertThat(json).contains("\"instances\" : [ ]");
    }

    @Test
    void pathsInJson_useForwardSlashes() {
        JwsInstallation inst = JwsInstallation.builder()
                .pid(1234)
                .catalinaBase(BASE)
                .build();
        MultiSummaryReport report = new MultiSummaryReport(1, List.of(inst));

        String json = formatter.format(report);

        assertThat(json).doesNotContain("\\\\");
    }

    private MultiSummaryReport emptyReport() {
        return new MultiSummaryReport(0, List.of());
    }
}
