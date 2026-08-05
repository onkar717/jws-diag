package org.jboss.jws.diag.logs;

import org.jboss.jws.diag.logs.model.LogMatch;
import org.jboss.jws.diag.logs.model.LogPattern;
import org.jboss.jws.diag.logs.model.LogScanResult;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LogScanner {

    // Cap sample lines per pattern to keep output readable for large log files.
    public static final int MAX_MATCHES_PER_PATTERN = 5;

    public LogScanResult scan(Path logFile) throws IOException {
        Map<LogPattern, Integer> counts = new EnumMap<>(LogPattern.class);
        Map<LogPattern, List<LogMatch>> matches = new EnumMap<>(LogPattern.class);
        for (LogPattern p : LogPattern.values()) {
            counts.put(p, 0);
            matches.put(p, new ArrayList<>());
        }

        long lineNumber = 0;
        try (BufferedReader reader = Files.newBufferedReader(logFile)) {
            String line;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                Set<LogPattern> lineMatches = EnumSet.noneOf(LogPattern.class);
                for (LogPattern p : LogPattern.values()) {
                    if (p.getRegex().matcher(line).find()) {
                        lineMatches.add(p);
                    }
                }
                // GC_OVERHEAD is a subtype of OOM — avoid double-counting a single exception line.
                if (lineMatches.contains(LogPattern.OOM)) {
                    lineMatches.remove(LogPattern.GC_OVERHEAD);
                }
                for (LogPattern p : lineMatches) {
                    counts.merge(p, 1, Integer::sum);
                    List<LogMatch> list = matches.get(p);
                    if (list.size() < MAX_MATCHES_PER_PATTERN) {
                        list.add(new LogMatch(lineNumber, line));
                    }
                }
            }
        }

        return new LogScanResult(logFile, lineNumber, counts, matches);
    }
}
