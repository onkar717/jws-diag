package org.jboss.jws.diag.summary.formatter;

import org.jboss.jws.diag.summary.model.JwsInstallation;
import org.jboss.jws.diag.summary.model.MultiSummaryReport;

/**
 * Renders a {@link MultiSummaryReport} as human-readable text.
 *
 * <p>Example:
 * <pre>
 * Auto-discovered 3 instance(s).
 *
 * Instance PID 1234  /opt/jws-6.0/standalone
 * ────────────────────────────────────────────────────────────────────────────────
 * Tomcat 10.1.49 | JWS 6.1.0
 * CATALINA_HOME: /opt/jws-6.0
 * ...
 *
 * Instance PID 5678  /opt/jws-5.7/standalone
 * ────────────────────────────────────────────────────────────────────────────────
 * ...
 * </pre>
 */
public final class MultiSummaryHumanFormatter {

    private static final String RULE = "─".repeat(80);
    private static final SummaryHumanFormatter SINGLE = new SummaryHumanFormatter();

    public String format(MultiSummaryReport report) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Auto-discovered %d instance(s).%n", report.getInstanceCount()));

        for (JwsInstallation inst : report.getInstances()) {
            sb.append('\n');
            String base = inst.getCatalinaBase() != null
                    ? inst.getCatalinaBase().toString().replace('\\', '/') : "N/A";
            String pid = inst.getPid() != null ? String.valueOf(inst.getPid()) : "?";
            sb.append(String.format("Instance PID %s  %s%n", pid, base));
            sb.append(RULE).append('\n');
            sb.append(SINGLE.format(inst));
        }

        return sb.toString();
    }
}
