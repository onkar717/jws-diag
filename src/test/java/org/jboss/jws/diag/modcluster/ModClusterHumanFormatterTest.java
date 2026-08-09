package org.jboss.jws.diag.modcluster;

import org.jboss.jws.diag.modcluster.formatter.ModClusterHumanFormatter;
import org.jboss.jws.diag.modcluster.model.ModClusterConfig;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ModClusterHumanFormatterTest {

    private final ModClusterHumanFormatter formatter = new ModClusterHumanFormatter();

    @Test
    void emptyList_showsNotFoundMessage() {
        String out = formatter.format(Collections.emptyList());
        assertThat(out).contains("No mod_cluster listener found");
        assertThat(out).contains("Tip:");
    }

    @Test
    void singleConfig_showsAllFields() {
        ModClusterConfig cfg = ModClusterConfig.builder()
                .listenerClassName("org.jboss.modcluster.container.catalina.standalone.ModClusterListener")
                .connector("ajp")
                .advertise(true)
                .advertiseGroupAddress("224.0.1.105")
                .advertisePort(23364)
                .proxyList("httpd1:6666")
                .balancer("mycluster")
                .stickySession(true)
                .stickySessionCookie("JSESSIONID")
                .build();

        String out = formatter.format(List.of(cfg));

        assertThat(out).contains("ModClusterListener");
        assertThat(out).contains("ajp");
        assertThat(out).contains("224.0.1.105:23364");
        assertThat(out).contains("httpd1:6666");
        assertThat(out).contains("mycluster");
        assertThat(out).contains("JSESSIONID");
    }

    @Test
    void noProxyList_showsAutoDiscoverMessage() {
        ModClusterConfig cfg = ModClusterConfig.builder()
                .listenerClassName("org.jboss.modcluster.ModClusterListener")
                .build();

        String out = formatter.format(List.of(cfg));
        assertThat(out).contains("auto-discover");
    }

    @Test
    void multipleListeners_countShown() {
        ModClusterConfig cfg = ModClusterConfig.builder()
                .listenerClassName("org.jboss.modcluster.ModClusterListener").build();

        String out = formatter.format(List.of(cfg, cfg));
        assertThat(out).contains("2 listener");
    }
}
