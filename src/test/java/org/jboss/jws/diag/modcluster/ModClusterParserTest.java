package org.jboss.jws.diag.modcluster;

import org.jboss.jws.diag.modcluster.model.ModClusterConfig;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ModClusterParserTest {

    private final ModClusterParser parser = new ModClusterParser();

    private Path fixture(String name) throws URISyntaxException {
        return Paths.get(getClass().getClassLoader()
                .getResource("fixtures/modcluster/" + name).toURI());
    }

    @Test
    void noModClusterListener_returnsEmptyList() throws Exception {
        List<ModClusterConfig> result = parser.parse(fixture("server-no-modcluster.xml"));
        assertThat(result).isEmpty();
    }

    @Test
    void fullConfig_allAttributesExtracted() throws Exception {
        List<ModClusterConfig> result = parser.parse(fixture("server-modcluster-full.xml"));

        assertThat(result).hasSize(1);
        ModClusterConfig cfg = result.get(0);
        assertThat(cfg.getListenerClassName())
                .isEqualTo("org.jboss.modcluster.container.catalina.standalone.ModClusterListener");
        assertThat(cfg.getConnector()).isEqualTo("ajp");
        assertThat(cfg.isAdvertise()).isTrue();
        assertThat(cfg.getAdvertiseGroupAddress()).isEqualTo("224.0.1.105");
        assertThat(cfg.getAdvertisePort()).isEqualTo(23364);
        assertThat(cfg.getProxyList()).isEqualTo("httpd1.example.com:6666,httpd2.example.com:6666");
        assertThat(cfg.getBalancer()).isEqualTo("prodcluster");
        assertThat(cfg.isStickySession()).isTrue();
        assertThat(cfg.getStickySessionCookie()).isEqualTo("JSESSIONID");
    }

    @Test
    void defaultsConfig_knownDefaultsApplied() throws Exception {
        List<ModClusterConfig> result = parser.parse(fixture("server-modcluster-defaults.xml"));

        assertThat(result).hasSize(1);
        ModClusterConfig cfg = result.get(0);
        assertThat(cfg.getConnector()).isEqualTo("ajp");
        assertThat(cfg.isAdvertise()).isTrue();
        assertThat(cfg.getAdvertiseGroupAddress()).isEqualTo("224.0.1.105");
        assertThat(cfg.getAdvertisePort()).isEqualTo(23364);
        assertThat(cfg.getProxyList()).isNull();
        assertThat(cfg.getBalancer()).isEqualTo("mycluster");
        assertThat(cfg.isStickySession()).isTrue();
        assertThat(cfg.getStickySessionCookie()).isEqualTo("JSESSIONID");
    }

    @Test
    void nonExistentFile_throwsIOException() {
        assertThatThrownBy(() -> parser.parse(Path.of("/nonexistent/server.xml")))
                .isInstanceOf(IOException.class);
    }
}
