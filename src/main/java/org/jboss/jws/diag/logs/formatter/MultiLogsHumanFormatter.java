package org.jboss.jws.diag.logs.formatter;

import org.jboss.jws.diag.logs.model.InstanceLogResult;
import org.jboss.jws.diag.logs.model.MultiLogReport;

/**
 * Renders a {@link MultiLogReport} as human-readable text.
 *
 * <p>Example:
 * <pre>
 * Auto-discovered 3 instance(s).
 *
 * Instance PID 1234  /opt/jws-6.0/standalone/logs/catalina.out
 * ────────────────────────────────────────────────────────────────────────────────
 * Log Scan: /opt/jws-6.0/standalone/logs/catalina.out
 * ...
 * </pre>
 */
public final class MultiLogsHumanFormatter {

    private static final String RULE = "─".repeat(80);
    private static final LogsHumanFormatter SINGLE = new LogsHumanFormatter();

    public String format(MultiLogReport report) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Auto-discovered %d instance(s).%n", report.getInstanceCount()));

        for (InstanceLogResult inst : report.getInstances()) {
            sb.append('\n');
            String logPath = inst.getLogFile().toString().replace('\\', '/');
            sb.append(String.format("Instance PID %d  %s%n", inst.getPid(), logPath));
            sb.append(RULE).append('\n');
            sb.append(SINGLE.format(inst.getResult()));
        }

        return sb.toString();
    }
}
