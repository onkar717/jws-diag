package org.jboss.jws.diag.diff.formatter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.jboss.jws.diag.diff.model.MultiDiffReport;

/**
 * Serializes a {@link MultiDiffReport} as indented JSON.
 *
 * <p>Schema:
 * <pre>
 * {
 *   "schemaVersion": "1.0",
 *   "referencePid": 1234,
 *   "referenceBase": "/opt/jws-6.0/standalone",
 *   "instanceCount": 3,
 *   "comparisons": [
 *     {
 *       "pid": 5678,
 *       "catalinaBase": "/opt/jws-5.7/standalone",
 *       "changeCount": 3,
 *       "changes": [ ... ]
 *     }
 *   ]
 * }
 * </pre>
 */
public final class MultiDiffJsonFormatter {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    public String format(MultiDiffReport report) {
        try {
            return MAPPER.writeValueAsString(report);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize MultiDiffReport to JSON", e);
        }
    }
}
