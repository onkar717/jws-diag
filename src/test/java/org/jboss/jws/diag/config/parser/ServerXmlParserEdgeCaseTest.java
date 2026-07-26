package org.jboss.jws.diag.config.parser;

import org.jboss.jws.diag.config.model.ExecutorConfig;
import org.jboss.jws.diag.config.model.ServiceConfig;
import org.jboss.jws.diag.config.model.ValveConfig;
import org.jboss.jws.diag.config.model.ValveType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ServerXmlParserEdgeCaseTest {

    private ServerXmlParser parser;

    @BeforeEach
    void setUp() {
        PropertyResolver resolver = new PropertyResolver(
                Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap());
        parser = new ServerXmlParser(resolver);
    }

    private Path fixture(String name) {
        try {
            return Paths.get(getClass().getClassLoader()
                    .getResource("fixtures/config/" + name).toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    // ── missing file ──────────────────────────────────────────────────────────

    @Test
    void missingServerXmlThrowsIoException(@TempDir Path tmpDir) {
        Path missing = tmpDir.resolve("server.xml");

        assertThatThrownBy(() -> parser.parse(missing))
                .isInstanceOf(IOException.class);
    }

    // ── non-numeric attributes silently default ───────────────────────────────

    @Test
    void nonNumericExecutorMaxThreadsDefaulted(@TempDir Path tmpDir) throws IOException {
        Path xml = tmpDir.resolve("server.xml");
        Files.writeString(xml,
                "<Server port=\"8005\" shutdown=\"SHUTDOWN\">" +
                "<Service name=\"Catalina\">" +
                "<Executor name=\"pool\" maxThreads=\"notANumber\" minSpareThreads=\"bad\"/>" +
                "<Connector port=\"8080\"/>" +
                "<Engine name=\"Catalina\" defaultHost=\"localhost\">" +
                "<Host name=\"localhost\"/>" +
                "</Engine>" +
                "</Service>" +
                "</Server>");

        ServiceConfig svc = parser.parse(xml).getServices().get(0);
        ExecutorConfig exec = svc.getExecutors().get(0);

        assertThat(exec.getMaxThreads().getValue()).isEqualTo(TomcatDefaults.EXECUTOR_MAX_THREADS);
        assertThat(exec.getMaxThreads().isExplicit()).isFalse();
        assertThat(exec.getMinSpareThreads().getValue()).isEqualTo(TomcatDefaults.EXECUTOR_MIN_SPARE_THREADS);
        assertThat(exec.getMinSpareThreads().isExplicit()).isFalse();
    }

    @Test
    void nonNumericConnectorMaxThreadsDefaulted(@TempDir Path tmpDir) throws IOException {
        Path xml = tmpDir.resolve("server.xml");
        Files.writeString(xml,
                "<Server port=\"8005\" shutdown=\"SHUTDOWN\">" +
                "<Service name=\"Catalina\">" +
                "<Connector port=\"8080\" maxThreads=\"notANumber\" connectionTimeout=\"bad\"/>" +
                "<Engine name=\"Catalina\" defaultHost=\"localhost\">" +
                "<Host name=\"localhost\"/>" +
                "</Engine>" +
                "</Service>" +
                "</Server>");

        var conn = parser.parse(xml).getServices().get(0).getConnectors().get(0);

        assertThat(conn.getMaxThreads().getValue()).isEqualTo(TomcatDefaults.CONNECTOR_MAX_THREADS);
        assertThat(conn.getMaxThreads().isExplicit()).isFalse();
        assertThat(conn.getConnectionTimeout().getValue()).isEqualTo(TomcatDefaults.CONNECTOR_CONNECTION_TIMEOUT);
        assertThat(conn.getConnectionTimeout().isExplicit()).isFalse();
    }

    // ── empty executor — all four attributes defaulted ────────────────────────

    @Test
    void emptyExecutorAllFourAttributesDefaulted() throws Exception {
        ServiceConfig svc = parser.parse(fixture("server-empty-executor.xml")).getServices().get(0);
        ExecutorConfig exec = svc.getExecutors().get(0);

        assertThat(exec.getName()).isEqualTo("minimalPool");
        assertThat(exec.getNamePrefix()).isNull();

        assertThat(exec.getMaxThreads().getValue()).isEqualTo(TomcatDefaults.EXECUTOR_MAX_THREADS);
        assertThat(exec.getMaxThreads().isExplicit()).isFalse();

        assertThat(exec.getMinSpareThreads().getValue()).isEqualTo(TomcatDefaults.EXECUTOR_MIN_SPARE_THREADS);
        assertThat(exec.getMinSpareThreads().isExplicit()).isFalse();

        assertThat(exec.getThreadPriority().getValue()).isEqualTo(TomcatDefaults.EXECUTOR_THREAD_PRIORITY);
        assertThat(exec.getThreadPriority().isExplicit()).isFalse();

        assertThat(exec.getMaxIdleTime().getValue()).isEqualTo(TomcatDefaults.EXECUTOR_MAX_IDLE_TIME);
        assertThat(exec.getMaxIdleTime().isExplicit()).isFalse();
    }

    // ── unknown valve ─────────────────────────────────────────────────────────

    @Test
    void unknownValveTypeIsNullInModel() throws Exception {
        var host = parser.parse(fixture("server-unknown-valve.xml"))
                .getServices().get(0).getEngine().getHosts().get(0);

        assertThat(host.getValves()).hasSize(2);

        ValveConfig unknown = host.getValves().get(0);
        assertThat(unknown.getClassName()).isEqualTo("com.example.CustomAuditValve");
        assertThat(unknown.getValveType()).isNull();

        ValveConfig known = host.getValves().get(1);
        assertThat(known.getValveType()).isEqualTo(ValveType.ACCESS_LOG);
    }

    @Test
    void unknownValveAttributesParsed() throws Exception {
        var host = parser.parse(fixture("server-unknown-valve.xml"))
                .getServices().get(0).getEngine().getHosts().get(0);

        ValveConfig unknown = host.getValves().get(0);
        assertThat(unknown.getAttributes()).containsKey("logFile");
        assertThat(unknown.getAttributes().get("logFile")).isEqualTo("audit.log");
    }

    // ── unresolved placeholder preserved verbatim ─────────────────────────────

    @Test
    void unresolvedPlaceholderPreservedInConnectorAttribute(@TempDir Path tmpDir) throws IOException {
        Path xml = tmpDir.resolve("server.xml");
        Files.writeString(xml,
                "<Server port=\"8005\" shutdown=\"SHUTDOWN\">" +
                "<Service name=\"Catalina\">" +
                "<Connector port=\"8080\" proxyName=\"${unresolved.host}\"/>" +
                "<Engine name=\"Catalina\" defaultHost=\"localhost\">" +
                "<Host name=\"localhost\"/>" +
                "</Engine>" +
                "</Service>" +
                "</Server>");

        var conn = parser.parse(xml).getServices().get(0).getConnectors().get(0);
        assertThat(conn.getProxyName()).isEqualTo("${unresolved.host}");
    }
}
