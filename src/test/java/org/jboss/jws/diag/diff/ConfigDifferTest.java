package org.jboss.jws.diag.diff;

import org.jboss.jws.diag.config.model.ConfigValue;
import org.jboss.jws.diag.config.model.ConnectorConfig;
import org.jboss.jws.diag.config.model.ExecutorConfig;
import org.jboss.jws.diag.config.model.ServerConfig;
import org.jboss.jws.diag.config.model.ServiceConfig;
import org.jboss.jws.diag.diff.model.ChangeType;
import org.jboss.jws.diag.diff.model.DiffEntry;
import org.jboss.jws.diag.diff.model.DiffReport;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigDifferTest {

    private static final Path LEFT = Path.of("/left");
    private static final Path RIGHT = Path.of("/right");
    private final ConfigDiffer differ = new ConfigDiffer();

    // ── helpers ─────────────────────────────────────────────────────────────

    private static ServerConfig server(int shutdownPort, List<ConnectorConfig> connectors,
                                       List<ExecutorConfig> executors) {
        return ServerConfig.builder()
                .shutdownPort(shutdownPort)
                .shutdownCommand("SHUTDOWN")
                .listeners(Collections.emptyList())
                .services(List.of(ServiceConfig.builder()
                        .name("Catalina")
                        .connectors(connectors)
                        .executors(executors)
                        .build()))
                .build();
    }

    private static ConnectorConfig connector(int port, int maxThreads, boolean explicit) {
        return ConnectorConfig.builder()
                .port(port)
                .protocol(ConfigValue.defaulted("HTTP/1.1"))
                .sslEnabled(ConfigValue.defaulted(false))
                .maxThreads(explicit
                        ? ConfigValue.explicit(maxThreads)
                        : ConfigValue.defaulted(maxThreads))
                .connectionTimeout(ConfigValue.defaulted(20000))
                .maxConnections(ConfigValue.defaulted(8192))
                .compression(ConfigValue.defaulted("off"))
                .secretRequired(ConfigValue.defaulted(false))
                .build();
    }

    private static ExecutorConfig executor(String name, int maxThreads) {
        return ExecutorConfig.builder()
                .name(name)
                .maxThreads(ConfigValue.explicit(maxThreads))
                .minSpareThreads(ConfigValue.defaulted(10))
                .threadPriority(ConfigValue.defaulted(5))
                .maxIdleTime(ConfigValue.defaulted(60000))
                .build();
    }

    // ── tests ────────────────────────────────────────────────────────────────

    @Test
    void identicalConfigs_noEntries() {
        ServerConfig left = server(8005, List.of(connector(8080, 200, false)), Collections.emptyList());
        ServerConfig right = server(8005, List.of(connector(8080, 200, false)), Collections.emptyList());

        DiffReport report = differ.diff(LEFT, RIGHT, left, right);

        assertThat(report.hasDifferences()).isFalse();
        assertThat(report.getEntries()).isEmpty();
    }

    @Test
    void shutdownPortChange_reportedAsChanged() {
        ServerConfig left = server(8005, Collections.emptyList(), Collections.emptyList());
        ServerConfig right = server(8006, Collections.emptyList(), Collections.emptyList());

        DiffReport report = differ.diff(LEFT, RIGHT, left, right);

        assertThat(report.hasDifferences()).isTrue();
        DiffEntry entry = report.getEntries().get(0);
        assertThat(entry.getPath()).isEqualTo("server.shutdownPort");
        assertThat(entry.getType()).isEqualTo(ChangeType.CHANGED);
        assertThat(entry.getLeftValue()).isEqualTo("8005");
        assertThat(entry.getRightValue()).isEqualTo("8006");
    }

    @Test
    void connectorMaxThreadsChange_reportedWithProvenance() {
        ServerConfig left = server(8005, List.of(connector(8080, 200, false)), Collections.emptyList());
        ServerConfig right = server(8005, List.of(connector(8080, 150, true)), Collections.emptyList());

        DiffReport report = differ.diff(LEFT, RIGHT, left, right);

        assertThat(report.getEntries())
                .extracting(DiffEntry::getPath)
                .contains("services[Catalina].connectors[8080].maxThreads");

        DiffEntry e = report.getEntries().stream()
                .filter(x -> x.getPath().endsWith(".maxThreads"))
                .findFirst().orElseThrow();
        assertThat(e.getLeftValue()).isEqualTo("200 (default)");
        assertThat(e.getRightValue()).isEqualTo("150 (explicit)");
    }

    @Test
    void connectorOnlyInRight_reportedAsAdded() {
        ServerConfig left = server(8005, List.of(connector(8080, 200, false)), Collections.emptyList());
        ServerConfig right = server(8005,
                List.of(connector(8080, 200, false), connector(8443, 200, false)),
                Collections.emptyList());

        DiffReport report = differ.diff(LEFT, RIGHT, left, right);

        DiffEntry e = report.getEntries().stream()
                .filter(x -> x.getPath().equals("services[Catalina].connectors[8443]"))
                .findFirst().orElseThrow();
        assertThat(e.getType()).isEqualTo(ChangeType.ADDED);
        assertThat(e.getLeftValue()).isNull();
    }

    @Test
    void connectorOnlyInLeft_reportedAsRemoved() {
        ServerConfig left = server(8005,
                List.of(connector(8080, 200, false), connector(8443, 200, false)),
                Collections.emptyList());
        ServerConfig right = server(8005, List.of(connector(8080, 200, false)), Collections.emptyList());

        DiffReport report = differ.diff(LEFT, RIGHT, left, right);

        DiffEntry e = report.getEntries().stream()
                .filter(x -> x.getPath().equals("services[Catalina].connectors[8443]"))
                .findFirst().orElseThrow();
        assertThat(e.getType()).isEqualTo(ChangeType.REMOVED);
        assertThat(e.getRightValue()).isNull();
    }

    @Test
    void executorOnlyInRight_reportedAsAdded() {
        ServerConfig left = server(8005, Collections.emptyList(), Collections.emptyList());
        ServerConfig right = server(8005, Collections.emptyList(), List.of(executor("pool", 150)));

        DiffReport report = differ.diff(LEFT, RIGHT, left, right);

        DiffEntry e = report.getEntries().stream()
                .filter(x -> x.getPath().equals("services[Catalina].executors[pool]"))
                .findFirst().orElseThrow();
        assertThat(e.getType()).isEqualTo(ChangeType.ADDED);
    }

    @Test
    void executorMaxThreadsChange_detected() {
        ServerConfig left = server(8005, Collections.emptyList(), List.of(executor("pool", 100)));
        ServerConfig right = server(8005, Collections.emptyList(), List.of(executor("pool", 200)));

        DiffReport report = differ.diff(LEFT, RIGHT, left, right);

        DiffEntry e = report.getEntries().stream()
                .filter(x -> x.getPath().endsWith("executors[pool].maxThreads"))
                .findFirst().orElseThrow();
        assertThat(e.getType()).isEqualTo(ChangeType.CHANGED);
        assertThat(e.getLeftValue()).isEqualTo("100 (explicit)");
        assertThat(e.getRightValue()).isEqualTo("200 (explicit)");
    }

    @Test
    void multipleChanges_allReported() {
        ServerConfig left = server(8005,
                List.of(connector(8080, 200, false)),
                List.of(executor("pool", 100)));
        ServerConfig right = server(8006,
                List.of(connector(8080, 150, true)),
                List.of(executor("pool", 200)));

        DiffReport report = differ.diff(LEFT, RIGHT, left, right);

        assertThat(report.getChangeCount()).isGreaterThanOrEqualTo(3);
    }

    @Test
    void leftAndRightPathsPreservedInReport() {
        ServerConfig cfg = server(8005, Collections.emptyList(), Collections.emptyList());
        DiffReport report = differ.diff(LEFT, RIGHT, cfg, cfg);

        assertThat(report.getLeft()).isEqualTo(LEFT.toString());
        assertThat(report.getRight()).isEqualTo(RIGHT.toString());
    }
}
