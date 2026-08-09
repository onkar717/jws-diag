package org.jboss.jws.diag.diff.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A single difference between two server.xml configurations.
 *
 * <p>{@code path} uses dot-notation, e.g.
 * {@code services[Catalina].connectors[8080].maxThreads}.
 * {@code leftValue} is null for ADDED entries; {@code rightValue} is null for REMOVED entries.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class DiffEntry {

    @JsonProperty("path")
    private final String path;

    @JsonProperty("type")
    private final ChangeType type;

    @JsonProperty("left")
    private final String leftValue;

    @JsonProperty("right")
    private final String rightValue;

    public DiffEntry(String path, ChangeType type, String leftValue, String rightValue) {
        this.path = path;
        this.type = type;
        this.leftValue = leftValue;
        this.rightValue = rightValue;
    }

    public String getPath() { return path; }
    public ChangeType getType() { return type; }
    public String getLeftValue() { return leftValue; }
    public String getRightValue() { return rightValue; }

    @Override
    public String toString() {
        return type + " " + path + " [" + leftValue + " → " + rightValue + "]";
    }
}
