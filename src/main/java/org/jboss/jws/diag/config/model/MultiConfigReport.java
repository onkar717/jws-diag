package org.jboss.jws.diag.config.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;

/**
 * Aggregates {@link InstanceConfigResult} entries from all running JWS instances
 * discovered via {@code /proc} scanning.
 */
@JsonPropertyOrder({"schemaVersion", "instanceCount", "instances"})
public final class MultiConfigReport {

    private static final String SCHEMA_VERSION = "1.0";

    private final int instanceCount;
    private final List<InstanceConfigResult> instances;

    public MultiConfigReport(int instanceCount, List<InstanceConfigResult> instances) {
        this.instanceCount = instanceCount;
        this.instances = List.copyOf(instances);
    }

    public String getSchemaVersion() {
        return SCHEMA_VERSION;
    }

    public int getInstanceCount() {
        return instanceCount;
    }

    public List<InstanceConfigResult> getInstances() {
        return instances;
    }
}
