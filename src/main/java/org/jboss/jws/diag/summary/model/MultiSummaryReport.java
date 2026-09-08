package org.jboss.jws.diag.summary.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;

/**
 * Aggregates {@link JwsInstallation} results from all running JWS instances
 * discovered via {@code /proc} scanning.
 */
@JsonPropertyOrder({"schemaVersion", "instanceCount", "instances"})
public final class MultiSummaryReport {

    private static final String SCHEMA_VERSION = "1.0";

    private final int instanceCount;
    private final List<JwsInstallation> instances;

    public MultiSummaryReport(int instanceCount, List<JwsInstallation> instances) {
        this.instanceCount = instanceCount;
        this.instances = List.copyOf(instances);
    }

    public String getSchemaVersion() {
        return SCHEMA_VERSION;
    }

    public int getInstanceCount() {
        return instanceCount;
    }

    public List<JwsInstallation> getInstances() {
        return instances;
    }
}
