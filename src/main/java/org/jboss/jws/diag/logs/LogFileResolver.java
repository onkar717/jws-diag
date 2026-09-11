package org.jboss.jws.diag.logs;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Works out which log file to scan for one running instance.
 *
 * <p>Resolution order:
 * <ol>
 *   <li>{@code CATALINA_OUT} from the instance's own environment, when it was exported</li>
 *   <li>{@code CATALINA_BASE/logs/catalina.out}</li>
 *   <li>The newest {@code catalina.*.log} in {@code CATALINA_BASE/logs}</li>
 * </ol>
 *
 * <p>Only regular files are returned. {@code CATALINA_OUT} is commonly pointed at
 * {@code /dev/stdout} in containers. Opening that would read this process's own
 * stdout rather than Tomcat's output, so it is reported as a skip instead.
 */
public final class LogFileResolver {

    private static final String LOGS_DIR = "logs";
    private static final String CATALINA_OUT_FILE = "catalina.out";
    private static final String DATED_LOG_GLOB = "catalina.*.log";

    private LogFileResolver() {
    }

    /**
     * @param catalinaBase the instance's {@code CATALINA_BASE}
     * @param catalinaOut  {@code CATALINA_OUT} from the instance's environment, or null if unset or unknown
     */
    public static Resolution resolve(Path catalinaBase, String catalinaOut) {
        if (catalinaOut != null && !catalinaOut.isBlank()) {
            return resolveConfigured(catalinaBase, catalinaOut);
        }

        Path logsDir = catalinaBase.resolve(LOGS_DIR);
        Path catalinaOutFile = logsDir.resolve(CATALINA_OUT_FILE);
        if (Files.isRegularFile(catalinaOutFile)) {
            return Resolution.found(catalinaOutFile);
        }

        Path newest = newestDatedLog(logsDir);
        if (newest != null) {
            return Resolution.found(newest);
        }
        return Resolution.skipped("no " + CATALINA_OUT_FILE + " or " + DATED_LOG_GLOB + " in " + logsDir);
    }

    // An explicit CATALINA_OUT is authoritative. Falling back to logs/catalina.out
    // here would scan a file Tomcat is no longer writing to, and report stale results.
    private static Resolution resolveConfigured(Path catalinaBase, String catalinaOut) {
        Path configured = Path.of(catalinaOut);
        if (!configured.isAbsolute()) {
            configured = catalinaBase.resolve(configured);
        }
        if (Files.isRegularFile(configured)) {
            return Resolution.found(configured);
        }
        if (!Files.exists(configured)) {
            return Resolution.skipped("CATALINA_OUT is " + catalinaOut + ", which does not exist");
        }
        return Resolution.skipped("CATALINA_OUT is " + catalinaOut
                + ", which is not a regular file; output likely goes to the container or journal");
    }

    // JULI's default file pattern is catalina.yyyy-MM-dd.log, so the greatest name
    // is the most recent day. Comparing names keeps this deterministic, unlike
    // modification times.
    private static Path newestDatedLog(Path logsDir) {
        if (!Files.isDirectory(logsDir)) {
            return null;
        }
        Path newest = null;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(logsDir, DATED_LOG_GLOB)) {
            for (Path candidate : stream) {
                if (!Files.isRegularFile(candidate)) {
                    continue;
                }
                if (newest == null || candidate.getFileName().toString()
                        .compareTo(newest.getFileName().toString()) > 0) {
                    newest = candidate;
                }
            }
        } catch (IOException e) {
            return null;
        }
        return newest;
    }

    /** Either a log file to scan, or the reason there is nothing to scan. */
    public static final class Resolution {

        private final Path logFile;
        private final String skipReason;

        private Resolution(Path logFile, String skipReason) {
            this.logFile = logFile;
            this.skipReason = skipReason;
        }

        static Resolution found(Path logFile) {
            return new Resolution(logFile, null);
        }

        static Resolution skipped(String reason) {
            return new Resolution(null, reason);
        }

        public boolean isFound() {
            return logFile != null;
        }

        public Path getLogFile() {
            return logFile;
        }

        public String getSkipReason() {
            return skipReason;
        }
    }
}
