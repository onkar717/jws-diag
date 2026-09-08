package org.jboss.jws.diag.diff;

import org.jboss.jws.diag.diff.formatter.MultiDiffJsonFormatter;
import org.jboss.jws.diag.diff.model.ChangeType;
import org.jboss.jws.diag.diff.model.DiffEntry;
import org.jboss.jws.diag.diff.model.DiffReport;
import org.jboss.jws.diag.diff.model.InstanceDiffResult;
import org.jboss.jws.diag.diff.model.MultiDiffReport;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MultiDiffJsonFormatterTest {

    private static final Path REF_BASE = Path.of("/opt/jws-6.0/standalone");
    private static final Path INST_BASE = Path.of("/opt/jws-5.7/standalone");
    private final MultiDiffJsonFormatter formatter = new MultiDiffJsonFormatter();

    @Test
    void output_containsSchemaVersion() {
        String json = formatter.format(emptyReport());

        assertThat(json).contains("\"schemaVersion\"");
        assertThat(json).contains("\"1.0\"");
    }

    @Test
    void output_containsReferencePidAndBase() {
        String json = formatter.format(emptyReport());

        assertThat(json).contains("\"referencePid\"");
        assertThat(json).contains("100");
        assertThat(json).contains("\"referenceBase\"");
    }

    @Test
    void output_containsInstanceCount() {
        MultiDiffReport report = new MultiDiffReport(100, REF_BASE, 2, Collections.emptyList());

        String json = formatter.format(report);

        assertThat(json).contains("\"instanceCount\" : 2");
    }

    @Test
    void output_containsComparisonsArray() {
        DiffEntry entry = new DiffEntry("server.shutdownPort", ChangeType.CHANGED, "8005", "8006");
        DiffReport diff = new DiffReport(REF_BASE, INST_BASE, List.of(entry));
        InstanceDiffResult result = new InstanceDiffResult(200, INST_BASE, diff);
        MultiDiffReport report = new MultiDiffReport(100, REF_BASE, 2, List.of(result));

        String json = formatter.format(report);

        assertThat(json).contains("\"comparisons\"");
        assertThat(json).contains("\"pid\" : 200");
        assertThat(json).contains("\"changeCount\" : 1");
        assertThat(json).contains("\"changes\"");
        assertThat(json).contains("shutdownPort");
    }

    @Test
    void identicalInstances_changeCountIsZero() {
        DiffReport diff = new DiffReport(REF_BASE, INST_BASE, Collections.emptyList());
        InstanceDiffResult result = new InstanceDiffResult(200, INST_BASE, diff);
        MultiDiffReport report = new MultiDiffReport(100, REF_BASE, 2, List.of(result));

        String json = formatter.format(report);

        assertThat(json).contains("\"changeCount\" : 0");
    }

    @Test
    void pathsInJson_useForwardSlashes() {
        MultiDiffReport report = new MultiDiffReport(100, REF_BASE, 1, Collections.emptyList());

        String json = formatter.format(report);

        assertThat(json).doesNotContain("\\\\");
    }

    private MultiDiffReport emptyReport() {
        return new MultiDiffReport(100, REF_BASE, 1, Collections.emptyList());
    }
}
