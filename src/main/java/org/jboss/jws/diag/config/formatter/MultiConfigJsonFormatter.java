package org.jboss.jws.diag.config.formatter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.jboss.jws.diag.config.model.MultiConfigReport;

/**
 * Serializes a {@link MultiConfigReport} as indented JSON.
 */
public final class MultiConfigJsonFormatter {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    public String format(MultiConfigReport report) {
        try {
            return MAPPER.writeValueAsString(report);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize MultiConfigReport to JSON", e);
        }
    }
}
