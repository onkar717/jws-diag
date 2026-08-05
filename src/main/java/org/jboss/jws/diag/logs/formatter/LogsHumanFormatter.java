package org.jboss.jws.diag.logs.formatter;

import org.jboss.jws.diag.logs.LogScanner;
import org.jboss.jws.diag.logs.model.LogMatch;
import org.jboss.jws.diag.logs.model.LogPattern;
import org.jboss.jws.diag.logs.model.LogScanResult;

import java.util.List;

public class LogsHumanFormatter {

    private static final String SEPARATOR = "─".repeat(52);
    private static final int MAX_LINE_WIDTH = 120;

    public String format(LogScanResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("Log Scan: ").append(result.getFile()).append('\n');
        sb.append("Lines scanned: ").append(result.getLinesScanned()).append('\n');
        sb.append(SEPARATOR).append('\n');

        for (LogPattern p : LogPattern.values()) {
            int count = result.countFor(p);
            sb.append(String.format("%-22s %4d", p.getLabel(), count));
            if (count > 0) {
                sb.append("  [").append(p.getSeverity()).append(']');
            }
            sb.append('\n');

            List<LogMatch> matches = result.matchesFor(p);
            for (LogMatch m : matches) {
                String text = m.getText().length() > MAX_LINE_WIDTH
                        ? m.getText().substring(0, MAX_LINE_WIDTH - 3) + "..."
                        : m.getText();
                sb.append(String.format("  line %6d  %s%n", m.getLineNumber(), text));
            }
            if (count > LogScanner.MAX_MATCHES_PER_PATTERN) {
                sb.append(String.format("  ... and %d more%n",
                        count - LogScanner.MAX_MATCHES_PER_PATTERN));
            }
        }

        return sb.toString().stripTrailing();
    }
}
