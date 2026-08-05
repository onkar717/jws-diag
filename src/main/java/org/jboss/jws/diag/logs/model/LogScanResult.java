package org.jboss.jws.diag.logs.model;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class LogScanResult {

    private final Path file;
    private final long linesScanned;
    private final Map<LogPattern, Integer> counts;
    private final Map<LogPattern, List<LogMatch>> matches;

    public LogScanResult(Path file, long linesScanned,
                         Map<LogPattern, Integer> counts,
                         Map<LogPattern, List<LogMatch>> matches) {
        this.file = file;
        this.linesScanned = linesScanned;
        this.counts = Collections.unmodifiableMap(counts);
        this.matches = Collections.unmodifiableMap(matches);
    }

    public Path getFile() { return file; }
    public long getLinesScanned() { return linesScanned; }
    public int countFor(LogPattern p) { return counts.getOrDefault(p, 0); }
    public List<LogMatch> matchesFor(LogPattern p) { return matches.getOrDefault(p, Collections.emptyList()); }
}
