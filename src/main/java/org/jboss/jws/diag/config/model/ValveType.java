package org.jboss.jws.diag.config.model;

/**
 * Classifies a {@code <Valve>} element by its well-known class name.
 *
 * <p>Allows formatters and downstream consumers to handle known valve types
 * without string-matching on fully-qualified class names.
 */
public enum ValveType {

    ACCESS_LOG(
            "org.apache.catalina.valves.AccessLogValve",
            "AccessLog"),
    REMOTE_IP(
            "org.apache.catalina.valves.RemoteIpValve",
            "RemoteIP"),
    STUCK_THREAD(
            "org.apache.catalina.valves.StuckThreadDetectionValve",
            "StuckThreadDetection"),
    ERROR_REPORT(
            "org.apache.catalina.valves.ErrorReportValve",
            "ErrorReport"),
    UNKNOWN(null, "Unknown");

    private final String className;
    private final String label;

    ValveType(String className, String label) {
        this.className = className;
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /**
     * Returns the {@code ValveType} matching the given fully-qualified class name,
     * or {@link #UNKNOWN} when the class is not recognised.
     */
    public static ValveType fromClassName(String cn) {
        if (cn == null) {
            return UNKNOWN;
        }
        for (ValveType t : values()) {
            if (t != UNKNOWN && cn.equals(t.className)) {
                return t;
            }
        }
        return UNKNOWN;
    }
}
