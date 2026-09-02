package org.jboss.jws.diag.diff.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.nio.file.Path;
import java.util.List;

/**
 * Diff result for one instance compared against the reference instance in a
 * multi-instance diff run.
 */
@JsonPropertyOrder({"pid", "catalinaBase", "changeCount", "changes"})
public final class InstanceDiffResult {

    private final int pid;
    private final Path catalinaBase;
    private final DiffReport diff;

    public InstanceDiffResult(int pid, Path catalinaBase, DiffReport diff) {
        this.pid = pid;
        this.catalinaBase = catalinaBase;
        this.diff = diff;
    }

    @JsonProperty("pid")
    public int getPid() { return pid; }

    @JsonProperty("catalinaBase")
    public String getCatalinaBase() { return catalinaBase.toString().replace('\\', '/'); }

    @JsonProperty("changeCount")
    public int getChangeCount() { return diff.getChangeCount(); }

    @JsonProperty("changes")
    public List<DiffEntry> getChanges() { return diff.getEntries(); }

    @JsonIgnore
    public DiffReport getDiff() { return diff; }

    @JsonIgnore
    public boolean hasDifferences() { return diff.hasDifferences(); }
}
