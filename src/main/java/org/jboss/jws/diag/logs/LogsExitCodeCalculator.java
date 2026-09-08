package org.jboss.jws.diag.logs;

import org.jboss.jws.diag.common.ExitCodes;
import org.jboss.jws.diag.common.Severity;
import org.jboss.jws.diag.logs.model.InstanceLogResult;
import org.jboss.jws.diag.logs.model.LogPattern;
import org.jboss.jws.diag.logs.model.LogScanResult;

import java.util.List;

/**
 * Maps log scan results onto the CLI exit code contract in {@link ExitCodes}.
 */
public final class LogsExitCodeCalculator {

    private LogsExitCodeCalculator() {
    }

    /**
     * Exit code for a single scanned log file, based on the highest severity matched.
     */
    public static int determineExitCode(LogScanResult result) {
        return highestSeverityCode(result, ExitCodes.OK);
    }

    /**
     * Exit code for a multi-instance scan.
     *
     * <p>An empty {@code results} list means every discovered instance was skipped, so
     * nothing was scanned at all; that is a failure rather than a clean run. When only
     * some instances were skipped the run is reported as a warning, matching the
     * partial-failure behaviour of {@code summary --all} and {@code config --all}.
     *
     * @param results    per-instance results for the instances that were scanned
     * @param discovered number of instances discovered before any were skipped
     */
    public static int determineMultiExitCode(List<InstanceLogResult> results, int discovered) {
        if (results.isEmpty()) {
            return ExitCodes.ERRORS;
        }

        int code = ExitCodes.OK;
        for (InstanceLogResult instanceResult : results) {
            code = highestSeverityCode(instanceResult.getResult(), code);
        }

        if (code == ExitCodes.OK && results.size() < discovered) {
            return ExitCodes.WARNINGS;
        }
        return code;
    }

    private static int highestSeverityCode(LogScanResult result, int current) {
        int code = current;
        for (LogPattern pattern : LogPattern.values()) {
            if (result.countFor(pattern) == 0) {
                continue;
            }
            if (pattern.getSeverity() == Severity.ERROR) {
                return ExitCodes.ERRORS;
            }
            if (pattern.getSeverity() == Severity.WARN && code < ExitCodes.WARNINGS) {
                code = ExitCodes.WARNINGS;
            }
        }
        return code;
    }
}
