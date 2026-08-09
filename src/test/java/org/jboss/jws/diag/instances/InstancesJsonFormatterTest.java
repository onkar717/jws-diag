package org.jboss.jws.diag.instances;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.jws.diag.instances.formatter.InstancesJsonFormatter;
import org.jboss.jws.diag.instances.model.TomcatInstance;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InstancesJsonFormatterTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final InstancesJsonFormatter formatter = new InstancesJsonFormatter();

    private JsonNode parse(List<TomcatInstance> instances) throws Exception {
        return MAPPER.readTree(formatter.format(instances));
    }

    @Test
    void schemaVersionPresentAtRoot() throws Exception {
        JsonNode root = parse(Collections.emptyList());
        assertThat(root.get("schemaVersion").asText()).isEqualTo("1.0");
    }

    @Test
    void countMatchesListSize() throws Exception {
        List<TomcatInstance> instances = List.of(
                new TomcatInstance(1, Path.of("/a"), Path.of("/a")),
                new TomcatInstance(2, Path.of("/b"), Path.of("/b"))
        );
        JsonNode root = parse(instances);

        assertThat(root.get("count").asInt()).isEqualTo(2);
        assertThat(root.get("instances").size()).isEqualTo(2);
    }

    @Test
    void emptyList_countZeroEmptyArray() throws Exception {
        JsonNode root = parse(Collections.emptyList());
        assertThat(root.get("count").asInt()).isEqualTo(0);
        assertThat(root.get("instances").size()).isEqualTo(0);
    }

    @Test
    void instanceEntry_hasPidAndPaths() throws Exception {
        List<TomcatInstance> instances = List.of(
                new TomcatInstance(12345, Path.of("/opt/tomcat"), Path.of("/opt/base"))
        );
        JsonNode entry = parse(instances).get("instances").get(0);

        assertThat(entry.get("pid").asInt()).isEqualTo(12345);
        assertThat(entry.get("catalinaHome").asText()).isEqualTo("/opt/tomcat");
        assertThat(entry.get("catalinaBase").asText()).isEqualTo("/opt/base");
    }
}
