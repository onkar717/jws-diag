package org.jboss.jws.diag.instances.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
    private final String catalinaOut;

    public TomcatInstance(int pid, Path catalinaHome, Path catalinaBase) {
        this(pid, catalinaHome, catalinaBase, null);
    }

    public TomcatInstance(int pid, Path catalinaHome, Path catalinaBase, String catalinaOut) {
        this.pid = pid;
        this.catalinaHome = catalinaHome;
        this.catalinaBase = catalinaBase;
        this.catalinaOut = catalinaOut;
    }

    @JsonProperty("pid")
    public int getPid() { return pid; }

    @JsonProperty("catalinaHome")
    @JsonSerialize(using = UnixPathSerializer.class)
    public Path getCatalinaHome() { return catalinaHome; }

    @JsonProperty("catalinaBase")
    @JsonSerialize(using = UnixPathSerializer.class)
    public Path getCatalinaBase() { return catalinaBase; }

    /**
     * {@code CATALINA_OUT} from the process environment, or null when it was not
     * exported or the environment could not be read. Used to locate the log file;
     * kept out of the JSON output so the {@code instances} schema does not change.
     */
    @JsonIgnore
    public String getCatalinaOut() { return catalinaOut; }
}
