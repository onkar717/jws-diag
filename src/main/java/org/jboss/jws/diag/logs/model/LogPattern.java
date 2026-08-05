package org.jboss.jws.diag.logs.model;

import org.jboss.jws.diag.common.Severity;

import java.util.regex.Pattern;

public enum LogPattern {
    OOM(
            "OOM",
            "OutOfMemoryError",
            Pattern.compile("java\\.lang\\.OutOfMemoryError"),
            Severity.ERROR
    ),
    BIND_EXCEPTION(
            "BIND_EXCEPTION",
            "BindException",
            Pattern.compile("java\\.net\\.BindException"),
            Severity.ERROR
    ),
    STUCK_THREAD(
            "STUCK_THREAD",
            "StuckThread",
            Pattern.compile("(?i)stuck thread|thread.*stuck"),
            Severity.WARN
    ),
    GC_OVERHEAD(
            "GC_OVERHEAD",
            "GC Overhead",
            Pattern.compile("GC overhead limit exceeded"),
            Severity.WARN
    ),
    CLASS_NOT_FOUND(
            "CLASS_NOT_FOUND",
            "ClassNotFound",
            Pattern.compile("java\\.lang\\.ClassNotFoundException"),
            Severity.INFO
    );

    private final String id;
    private final String label;
    private final Pattern regex;
    private final Severity severity;

    LogPattern(String id, String label, Pattern regex, Severity severity) {
        this.id = id;
        this.label = label;
        this.regex = regex;
        this.severity = severity;
    }

    public String getId() { return id; }
    public String getLabel() { return label; }
    public Pattern getRegex() { return regex; }
    public Severity getSeverity() { return severity; }
}
