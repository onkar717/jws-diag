package org.jboss.jws.diag.logs.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.jboss.jws.diag.common.UnixPathSerializer;

import java.nio.file.Path;

/**
 * Pairs a running instance's PID and log file path with its {@link LogScanResult},
 * for use in multi-instance log reporting.
 */
@JsonPropertyOrder({"pid", "logFile", "result"})
public final class InstanceLogResult {

    private final int pid;
    private final Path logFile;
    private final LogScanResult result;

    public InstanceLogResult(int pid, Path logFile, LogScanResult result) {
        this.pid = pid;
        this.logFile = logFile;
        this.result = result;
    }

    public int getPid() {
        return pid;
    }

    @JsonSerialize(using = UnixPathSerializer.class)
    public Path getLogFile() {
        return logFile;
    }

    public LogScanResult getResult() {
        return result;
    }
}
