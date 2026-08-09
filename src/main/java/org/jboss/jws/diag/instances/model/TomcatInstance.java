package org.jboss.jws.diag.instances.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jboss.jws.diag.common.UnixPathSerializer;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.nio.file.Path;

/**
 * A single running Tomcat/JWS process discovered on this host.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class TomcatInstance {

    private final int pid;
    private final Path catalinaHome;
    private final Path catalinaBase;

    public TomcatInstance(int pid, Path catalinaHome, Path catalinaBase) {
        this.pid = pid;
        this.catalinaHome = catalinaHome;
        this.catalinaBase = catalinaBase;
    }

    @JsonProperty("pid")
    public int getPid() { return pid; }

    @JsonProperty("catalinaHome")
    @JsonSerialize(using = UnixPathSerializer.class)
    public Path getCatalinaHome() { return catalinaHome; }

    @JsonProperty("catalinaBase")
    @JsonSerialize(using = UnixPathSerializer.class)
    public Path getCatalinaBase() { return catalinaBase; }
}
