package org.jboss.jws.diag.logs;

import org.jboss.jws.diag.common.ExitCodes;
import org.jboss.jws.diag.common.OutputFormatMixin;
import org.jboss.jws.diag.common.Severity;
import org.jboss.jws.diag.logs.formatter.LogsHumanFormatter;
import org.jboss.jws.diag.logs.formatter.LogsJsonFormatter;
import org.jboss.jws.diag.logs.model.LogPattern;
import org.jboss.jws.diag.logs.model.LogScanResult;
import org.jboss.jws.diag.summary.discovery.CatalinaDiscovery;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Command(name = "logs",
        description = "Scan Tomcat log files for known error patterns (OOM, BindException, StuckThread, etc.)",
        mixinStandardHelpOptions = true)
public class LogsCommand implements Runnable {

    @Option(names = "--log-file",
            description = "Path to log file to scan (default: CATALINA_BASE/logs/catalina.out)")
    private Path logFile;

    @Option(names = "--catalina-home", description = "Path to CATALINA_HOME (overrides auto-detection)")
    private Path catalinaHome;

    @Option(names = "--catalina-base", description = "Path to CATALINA_BASE (defaults to CATALINA_HOME)")
    private Path catalinaBase;

    @Mixin
    private OutputFormatMixin outputFormat;

    @Override
    public void run() {
        Path target = resolveLogFile();
        if (target == null) {
            System.err.println("ERROR: Could not determine log file path. "
                    + "Use --log-file or --catalina-home/--catalina-base.");
            System.exit(ExitCodes.ERRORS);
            return;
        }
        if (!Files.exists(target)) {
            System.err.println("ERROR: Log file not found: " + target);
            System.exit(ExitCodes.ERRORS);
            return;
        }

        LogScanResult result;
        try {
            result = new LogScanner().scan(target);
        } catch (IOException e) {
            System.err.println("ERROR: Failed to scan log file: " + e.getMessage());
            System.exit(ExitCodes.ERRORS);
            return;
        }

        switch (outputFormat.getFormat()) {
            case HUMAN:
                System.out.println(new LogsHumanFormatter().format(result));
                break;
            case JSON:
                System.out.println(new LogsJsonFormatter().format(result));
                break;
        }

        System.exit(determineExitCode(result));
    }

    private Path resolveLogFile() {
        if (logFile != null) {
            return logFile;
        }
        Path base = resolveBase();
        return base != null ? base.resolve("logs/catalina.out") : null;
    }

    private Path resolveBase() {
        if (catalinaBase != null) return catalinaBase;
        if (catalinaHome != null) return catalinaHome;
        CatalinaDiscovery.Result result = CatalinaDiscovery.create(null, null).discover();
        return result.getCatalinaBase() != null ? result.getCatalinaBase() : result.getCatalinaHome();
    }

    private int determineExitCode(LogScanResult result) {
        for (LogPattern p : LogPattern.values()) {
            if (result.countFor(p) > 0) {
                if (p.getSeverity() == Severity.ERROR) return ExitCodes.ERRORS;
            }
        }
        for (LogPattern p : LogPattern.values()) {
            if (result.countFor(p) > 0) {
                if (p.getSeverity() == Severity.WARN) return ExitCodes.WARNINGS;
            }
        }
        return ExitCodes.OK;
    }
}
