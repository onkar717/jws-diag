package org.jboss.jws.diag.diff;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.jws.diag.diff.formatter.DiffJsonFormatter;
import org.jboss.jws.diag.diff.model.ChangeType;
import org.jboss.jws.diag.diff.model.DiffEntry;
import org.jboss.jws.diag.diff.model.DiffReport;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DiffJsonFormatterTest {

    private static final Path LEFT = Path.of("/opt/tomcat-a");
    private static final Path RIGHT = Path.of("/opt/tomcat-b");
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final DiffJsonFormatter formatter = new DiffJsonFormatter();

    private JsonNode parse(DiffReport report) throws Exception {
        return MAPPER.readTree(formatter.format(report));
    }

    @Test
    void schemaVersionPresentAtRoot() throws Exception {
        DiffReport report = new DiffReport(LEFT, RIGHT, Collections.emptyList());
        JsonNode root = parse(report);

        assertThat(root.get("schemaVersion").asText()).isEqualTo("1.0");
    }

    @Test
    void leftAndRightPathsInRoot() throws Exception {
        DiffReport report = new DiffReport(LEFT, RIGHT, Collections.emptyList());
        JsonNode root = parse(report);

        assertThat(root.get("left").asText()).isEqualTo("/opt/tomcat-a");
        assertThat(root.get("right").asText()).isEqualTo("/opt/tomcat-b");
    }

    @Test
    void changeCountMatchesEntriesSize() throws Exception {
        List<DiffEntry> entries = List.of(
                new DiffEntry("server.shutdownPort", ChangeType.CHANGED, "8005", "8006")
        );
        DiffReport report = new DiffReport(LEFT, RIGHT, entries);
        JsonNode root = parse(report);

        assertThat(root.get("changeCount").asInt()).isEqualTo(1);
        assertThat(root.get("changes").size()).isEqualTo(1);
    }

    @Test
    void changedEntry_hasAllFields() throws Exception {
        DiffEntry entry = new DiffEntry("server.shutdownPort", ChangeType.CHANGED, "8005", "8006");
        JsonNode root = parse(new DiffReport(LEFT, RIGHT, List.of(entry)));

        JsonNode change = root.get("changes").get(0);
        assertThat(change.get("path").asText()).isEqualTo("server.shutdownPort");
        assertThat(change.get("type").asText()).isEqualTo("CHANGED");
        assertThat(change.get("left").asText()).isEqualTo("8005");
        assertThat(change.get("right").asText()).isEqualTo("8006");
    }

    @Test
    void addedEntry_rightPresentLeftAbsent() throws Exception {
        DiffEntry entry = new DiffEntry("services[Catalina].connectors[8443]",
                ChangeType.ADDED, null, "port 8443");
        JsonNode root = parse(new DiffReport(LEFT, RIGHT, List.of(entry)));

        JsonNode change = root.get("changes").get(0);
        assertThat(change.get("type").asText()).isEqualTo("ADDED");
        assertThat(change.has("left")).isFalse();
        assertThat(change.get("right").asText()).isEqualTo("port 8443");
    }

    @Test
    void emptyDiff_changesIsEmptyArray() throws Exception {
        JsonNode root = parse(new DiffReport(LEFT, RIGHT, Collections.emptyList()));

        assertThat(root.get("changes").isArray()).isTrue();
        assertThat(root.get("changes").size()).isEqualTo(0);
        assertThat(root.get("changeCount").asInt()).isEqualTo(0);
    }
}
