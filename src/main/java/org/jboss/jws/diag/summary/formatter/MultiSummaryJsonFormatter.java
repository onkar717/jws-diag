package org.jboss.jws.diag.summary.formatter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.jboss.jws.diag.summary.model.MultiSummaryReport;

/**
 * Serializes a {@link MultiSummaryReport} as indented JSON.
 */
public final class MultiSummaryJsonFormatter {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    public String format(MultiSummaryReport report) {
        try {
            return MAPPER.writeValueAsString(report);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize MultiSummaryReport to JSON", e);
        }
    }
}
