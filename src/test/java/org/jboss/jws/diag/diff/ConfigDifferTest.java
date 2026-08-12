package org.jboss.jws.diag.diff;

import org.jboss.jws.diag.config.model.CertificateConfig;
import org.jboss.jws.diag.config.model.ConfigValue;
import org.jboss.jws.diag.config.model.ConnectorConfig;
import org.jboss.jws.diag.config.model.ExecutorConfig;
import org.jboss.jws.diag.config.model.ServerConfig;
import org.jboss.jws.diag.config.model.ServiceConfig;
import org.jboss.jws.diag.config.model.SslHostConfig;
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

    // ── SSL / certificate / shutdownCommand ──────────────────────────────────

    @Test
    void shutdownCommandChange_reportedAsChanged() {
        ServerConfig left = ServerConfig.builder()
                .shutdownPort(8005).shutdownCommand("SHUTDOWN")
                .listeners(Collections.emptyList())
                .services(List.of(ServiceConfig.builder()
                        .name("Catalina").connectors(Collections.emptyList())
                        .executors(Collections.emptyList()).build()))
                .build();
        ServerConfig right = ServerConfig.builder()
                .shutdownPort(8005).shutdownCommand("STOP")
                .listeners(Collections.emptyList())
                .services(List.of(ServiceConfig.builder()
                        .name("Catalina").connectors(Collections.emptyList())
                        .executors(Collections.emptyList()).build()))
                .build();

        DiffReport report = differ.diff(LEFT, RIGHT, left, right);

        DiffEntry entry = report.getEntries().stream()
                .filter(e -> e.getPath().equals("server.shutdownCommand"))
                .findFirst().orElseThrow();
        assertThat(entry.getType()).isEqualTo(ChangeType.CHANGED);
        assertThat(entry.getLeftValue()).isEqualTo("SHUTDOWN");
        assertThat(entry.getRightValue()).isEqualTo("STOP");
    }

    @Test
    void sslHostConfigAddedInRight_reportedAsAdded() {
        ServerConfig left = server(8005,
                List.of(connector(8443, 200, false)), Collections.emptyList());
        ServerConfig right = server(8005,
                List.of(sslConnector(8443, List.of(ssl("_default_", "TLSv1.2,TLSv1.3",
                        Collections.emptyList())))),
                Collections.emptyList());

        DiffReport report = differ.diff(LEFT, RIGHT, left, right);

        DiffEntry entry = report.getEntries().stream()
                .filter(e -> e.getPath().contains(".ssl[") && e.getType() == ChangeType.ADDED)
                .findFirst().orElseThrow();
        assertThat(entry.getRightValue()).isEqualTo("_default_");
    }

    @Test
    void sslHostConfigProtocolChange_reportedAsChanged() {
        ServerConfig left = server(8005,
                List.of(sslConnector(8443, List.of(ssl("_default_", "TLSv1.2", Collections.emptyList())))),
                Collections.emptyList());
        ServerConfig right = server(8005,
                List.of(sslConnector(8443, List.of(ssl("_default_", "TLSv1.2,TLSv1.3",
                        Collections.emptyList())))),
                Collections.emptyList());

        DiffReport report = differ.diff(LEFT, RIGHT, left, right);

        DiffEntry entry = report.getEntries().stream()
                .filter(e -> e.getPath().endsWith(".protocols"))
                .findFirst().orElseThrow();
        assertThat(entry.getType()).isEqualTo(ChangeType.CHANGED);
        assertThat(entry.getLeftValue()).isEqualTo("TLSv1.2");
        assertThat(entry.getRightValue()).isEqualTo("TLSv1.2,TLSv1.3");
    }

    @Test
    void certificateAddedInRight_reportedAsAdded() {
        CertificateConfig cert = CertificateConfig.builder()
                .keystoreFile("conf/server.jks")
                .keystoreType(ConfigValue.explicit("JKS"))
                .type("RSA")
                .build();
        ServerConfig left = server(8005,
                List.of(sslConnector(8443, List.of(ssl("_default_", "TLSv1.2",
                        Collections.emptyList())))),
                Collections.emptyList());
        ServerConfig right = server(8005,
                List.of(sslConnector(8443, List.of(ssl("_default_", "TLSv1.2",
                        List.of(cert))))),
                Collections.emptyList());

        DiffReport report = differ.diff(LEFT, RIGHT, left, right);

        DiffEntry entry = report.getEntries().stream()
                .filter(e -> e.getPath().contains(".certificate[") && e.getType() == ChangeType.ADDED)
                .findFirst().orElseThrow();
        assertThat(entry.getRightValue()).isEqualTo("RSA");
    }

    @Test
    void certificateKeystoreFileChange_reportedAsChanged() {
        CertificateConfig leftCert = CertificateConfig.builder()
                .keystoreFile("conf/old.jks")
                .keystoreType(ConfigValue.explicit("JKS"))
                .type("RSA")
                .build();
        CertificateConfig rightCert = CertificateConfig.builder()
                .keystoreFile("conf/new.jks")
                .keystoreType(ConfigValue.explicit("JKS"))
                .type("RSA")
                .build();
        ServerConfig left = server(8005,
                List.of(sslConnector(8443, List.of(ssl("_default_", "TLSv1.2", List.of(leftCert))))),
                Collections.emptyList());
        ServerConfig right = server(8005,
                List.of(sslConnector(8443, List.of(ssl("_default_", "TLSv1.2", List.of(rightCert))))),
                Collections.emptyList());

        DiffReport report = differ.diff(LEFT, RIGHT, left, right);

        DiffEntry entry = report.getEntries().stream()
                .filter(e -> e.getPath().endsWith(".keystoreFile"))
                .findFirst().orElseThrow();
        assertThat(entry.getType()).isEqualTo(ChangeType.CHANGED);
        assertThat(entry.getLeftValue()).isEqualTo("conf/old.jks");
        assertThat(entry.getRightValue()).isEqualTo("conf/new.jks");
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private static ConnectorConfig sslConnector(int port, List<SslHostConfig> sslHostConfigs) {
        return ConnectorConfig.builder()
                .port(port)
                .protocol(ConfigValue.explicit("org.apache.coyote.http11.Http11NioProtocol"))
                .sslEnabled(ConfigValue.explicit(true))
                .maxThreads(ConfigValue.defaulted(200))
                .connectionTimeout(ConfigValue.defaulted(20000))
                .maxConnections(ConfigValue.defaulted(8192))
                .compression(ConfigValue.defaulted("off"))
                .secretRequired(ConfigValue.defaulted(false))
                .sslHostConfigs(sslHostConfigs)
                .build();
    }

    private static SslHostConfig ssl(String hostName, String protocols,
                                     List<CertificateConfig> certs) {
        return SslHostConfig.builder()
                .hostName(hostName)
                .protocols(protocols)
                .certificates(certs)
                .build();
    }
}
