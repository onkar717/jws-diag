package org.jboss.jws.diag.config.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.jboss.jws.diag.common.UnixPathSerializer;

import java.nio.file.Path;

/**
 * Pairs a running instance's PID and {@code CATALINA_BASE} with its parsed
 * {@link ServerConfig}, for use in multi-instance config reporting.
 */
@JsonPropertyOrder({"pid", "catalinaBase", "config"})
public final class InstanceConfigResult {

    private final int pid;
    private final Path catalinaBase;
    private final ServerConfig config;

    public InstanceConfigResult(int pid, Path catalinaBase, ServerConfig config) {
        this.pid = pid;
        this.catalinaBase = catalinaBase;
        this.config = config;
    }

    public int getPid() {
        return pid;
    }

    @JsonSerialize(using = UnixPathSerializer.class)
    public Path getCatalinaBase() {
        return catalinaBase;
    }

    public ServerConfig getConfig() {
        return config;
    }
}
