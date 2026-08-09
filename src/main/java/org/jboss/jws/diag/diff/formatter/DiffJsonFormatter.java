package org.jboss.jws.diag.diff.formatter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.jboss.jws.diag.diff.model.DiffReport;

/**
 * Serializes a {@link DiffReport} as indented JSON.
 *
 * <p>Schema:
 * <pre>
 * {
 *   "schemaVersion": "1.0",
 *   "left": "/opt/tomcat-a",
 *   "right": "/opt/tomcat-b",
 *   "changeCount": 2,
 *   "changes": [
 *     { "path": "server.shutdownPort", "type": "CHANGED", "left": "8005", "right": "8006" },
 *     ...
 *   ]
 * }
 * </pre>
 */
public final class DiffJsonFormatter {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    public String format(DiffReport report) {
        try {
            return MAPPER.writeValueAsString(report);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize DiffReport to JSON", e);
        }
    }
}
