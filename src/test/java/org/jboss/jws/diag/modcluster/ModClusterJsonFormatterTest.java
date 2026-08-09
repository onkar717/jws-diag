package org.jboss.jws.diag.modcluster;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.jws.diag.modcluster.formatter.ModClusterJsonFormatter;
import org.jboss.jws.diag.modcluster.model.ModClusterConfig;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ModClusterJsonFormatterTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final ModClusterJsonFormatter formatter = new ModClusterJsonFormatter();

    private JsonNode parse(List<ModClusterConfig> configs) throws Exception {
        return MAPPER.readTree(formatter.format(configs));
    }

    @Test
    void schemaVersionPresentAtRoot() throws Exception {
        JsonNode root = parse(Collections.emptyList());
        assertThat(root.get("schemaVersion").asText()).isEqualTo("1.0");
    }

    @Test
    void emptyList_countZeroEmptyArray() throws Exception {
        JsonNode root = parse(Collections.emptyList());
        assertThat(root.get("count").asInt()).isEqualTo(0);
        assertThat(root.get("listeners").size()).isEqualTo(0);
    }

    @Test
    void singleConfig_allKnownFieldsPresent() throws Exception {
        ModClusterConfig cfg = ModClusterConfig.builder()
                .listenerClassName("org.jboss.modcluster.container.catalina.standalone.ModClusterListener")
                .connector("ajp")
                .proxyList("httpd1:6666")
                .balancer("mycluster")
                .build();

        JsonNode entry = parse(List.of(cfg)).get("listeners").get(0);
        assertThat(entry.get("listenerClassName").asText()).contains("ModClusterListener");
        assertThat(entry.get("connector").asText()).isEqualTo("ajp");
        assertThat(entry.get("proxyList").asText()).isEqualTo("httpd1:6666");
        assertThat(entry.get("balancer").asText()).isEqualTo("mycluster");
    }

    @Test
    void countMatchesListSize() throws Exception {
        ModClusterConfig cfg = ModClusterConfig.builder()
                .listenerClassName("org.jboss.modcluster.ModClusterListener").build();

        JsonNode root = parse(List.of(cfg, cfg));
        assertThat(root.get("count").asInt()).isEqualTo(2);
        assertThat(root.get("listeners").size()).isEqualTo(2);
    }

    @Test
    void nullProxyList_fieldAbsentFromJson() throws Exception {
        ModClusterConfig cfg = ModClusterConfig.builder()
                .listenerClassName("org.jboss.modcluster.ModClusterListener").build();

        JsonNode entry = parse(List.of(cfg)).get("listeners").get(0);
        assertThat(entry.has("proxyList")).isFalse();
    }
}
