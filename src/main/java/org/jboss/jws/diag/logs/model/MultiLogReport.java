package org.jboss.jws.diag.logs.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;

/**
 * Aggregates {@link InstanceLogResult} entries from all running JWS instances
 * discovered via {@code /proc} scanning.
 */
@JsonPropertyOrder({"schemaVersion", "instanceCount", "instances"})
public final class MultiLogReport {

    private static final String SCHEMA_VERSION = "1.0";

    private final int instanceCount;
    private final List<InstanceLogResult> instances;

    public MultiLogReport(int instanceCount, List<InstanceLogResult> instances) {
        this.instanceCount = instanceCount;
        this.instances = List.copyOf(instances);
    }

    public String getSchemaVersion() {
        return SCHEMA_VERSION;
    }

    public int getInstanceCount() {
        return instanceCount;
    }

    public List<InstanceLogResult> getInstances() {
        return instances;
    }
}
