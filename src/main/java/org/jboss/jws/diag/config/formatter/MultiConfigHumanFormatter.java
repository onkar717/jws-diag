package org.jboss.jws.diag.config.formatter;

import org.jboss.jws.diag.config.model.InstanceConfigResult;
import org.jboss.jws.diag.config.model.MultiConfigReport;

/**
 * Renders a {@link MultiConfigReport} as human-readable text.
 *
 * <p>Example:
 * <pre>
 * Auto-discovered 3 instance(s).
 *
 * Instance PID 1234  /opt/jws-6.0/standalone
 * ────────────────────────────────────────────────────────────────────────────────
 * connectors:
 *   ...
 *
 * Instance PID 5678  /opt/jws-5.7/standalone
 * ────────────────────────────────────────────────────────────────────────────────
 * ...
 * </pre>
 */
public final class MultiConfigHumanFormatter {

    private static final String RULE = "─".repeat(80);
    private static final ConfigHumanFormatter SINGLE = new ConfigHumanFormatter();

    public String format(MultiConfigReport report) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Auto-discovered %d instance(s).%n", report.getInstanceCount()));

        for (InstanceConfigResult result : report.getInstances()) {
            sb.append('\n');
            String base = result.getCatalinaBase().toString().replace('\\', '/');
            sb.append(String.format("Instance PID %d  %s%n", result.getPid(), base));
            sb.append(RULE).append('\n');
            sb.append(SINGLE.format(result.getConfig()));
        }

        return sb.toString();
    }
}
