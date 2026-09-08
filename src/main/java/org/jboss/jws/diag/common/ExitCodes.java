package org.jboss.jws.diag.common;

/**
 * The exit code contract for every jws-diag command.
 *
 * <p>Codes 0 to 2 describe what the tool found. Code 3 describes the tool failing to
 * look at all. Scripts and CI gates can therefore tell "your configuration has a
 * problem" apart from "jws-diag could not run", which call for opposite responses.
 *
 * <table>
 *   <caption>Exit code meanings</caption>
 *   <tr><th>Code</th><th>Meaning</th></tr>
 *   <tr><td>{@link #OK}</td><td>Ran successfully; nothing noteworthy found</td></tr>
 *   <tr><td>{@link #WARNINGS}</td><td>Ran successfully; warning level findings, a
 *       non-empty difference, or a partially collected result</td></tr>
 *   <tr><td>{@link #ERRORS}</td><td>Ran successfully; error level findings</td></tr>
 *   <tr><td>{@link #TOOL_FAILURE}</td><td>The tool itself failed: installation not
 *       found, file unreadable, malformed XML, or bad arguments</td></tr>
 * </table>
 *
 * <p>Absence of optional configuration is a result, not a failure. {@code modcluster}
 * with no mod_cluster listener and {@code instances} with no running instance both
 * report {@link #OK}.
 */
public final class ExitCodes {

    /** Ran successfully; nothing noteworthy found. */
    public static final int OK = 0;

    /**
     * Ran successfully, with something worth attention: warning level findings, a
     * non-empty difference, or a result that is usable but incomplete.
     */
    public static final int WARNINGS = 1;

    /** Ran successfully; error level findings. */
    public static final int ERRORS = 2;

    /**
     * The tool could not complete: installation not found, file unreadable, malformed
     * XML, or bad arguments. Never used to report something found in a configuration.
     */
    public static final int TOOL_FAILURE = 3;

    private ExitCodes() {
    }
}
