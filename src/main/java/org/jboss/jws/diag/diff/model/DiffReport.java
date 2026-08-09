package org.jboss.jws.diag.diff.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

/**
 * Full diff result between two server.xml configurations.
 */
@JsonPropertyOrder({"schemaVersion", "left", "right", "changeCount", "changes"})
public final class DiffReport {

    @JsonProperty("schemaVersion")
    private static final String SCHEMA_VERSION = "1.0";

    private final Path leftBase;
    private final Path rightBase;
    private final List<DiffEntry> entries;

    public DiffReport(Path leftBase, Path rightBase, List<DiffEntry> entries) {
        this.leftBase = leftBase;
        this.rightBase = rightBase;
        this.entries = Collections.unmodifiableList(entries);
    }

    @JsonProperty("schemaVersion")
    public String getSchemaVersion() { return SCHEMA_VERSION; }

    @JsonProperty("left")
    public String getLeft() { return leftBase.toString(); }

    @JsonProperty("right")
    public String getRight() { return rightBase.toString(); }

    @JsonProperty("changeCount")
    public int getChangeCount() { return entries.size(); }

    @JsonProperty("changes")
    public List<DiffEntry> getEntries() { return entries; }

    public boolean hasDifferences() { return !entries.isEmpty(); }
}
