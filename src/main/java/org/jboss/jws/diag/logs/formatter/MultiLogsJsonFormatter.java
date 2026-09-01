package org.jboss.jws.diag.logs.formatter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.jboss.jws.diag.logs.model.MultiLogReport;

/**
 * Serializes a {@link MultiLogReport} as indented JSON.
 */
public final class MultiLogsJsonFormatter {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    public String format(MultiLogReport report) {
        try {
            return MAPPER.writeValueAsString(report);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize MultiLogReport to JSON", e);
        }
    }
}
