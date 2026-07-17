package org.jboss.jws.diag.config.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.Collections;
import java.util.Map;

/**
 * Represents a {@code <Valve>} element in {@code server.xml}.
 *
 * <p>{@link #getValveType()} classifies the valve by its class name for formatters
 * and downstream consumers that need to handle well-known valve types without
 * matching on fully-qualified class name strings.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"valveType", "className", "attributes"})
public final class ValveConfig {

    private final String className;
    private final Map<String, String> attributes;

    private ValveConfig(Builder b) {
        this.className = b.className;
        this.attributes = b.attributes != null
                ? Collections.unmodifiableMap(b.attributes) : Collections.emptyMap();
    }

    public String getClassName() { return className; }

    /**
     * Returns the {@link ValveType} matching this valve's class name,
     * or {@code null} when the class is not a well-known type (suppressed in JSON output).
     */
    public ValveType getValveType() {
        ValveType t = ValveType.fromClassName(className);
        return t == ValveType.UNKNOWN ? null : t;
    }

    public Map<String, String> getAttributes() { return attributes; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String className;
        private Map<String, String> attributes;

        public Builder className(String v) { this.className = v; return this; }
        public Builder attributes(Map<String, String> v) { this.attributes = v; return this; }

        public ValveConfig build() { return new ValveConfig(this); }
    }

    @Override
    public String toString() {
        return "ValveConfig{valveType=" + getValveType()
                + ", className='" + className + "', attributes=" + attributes + '}';
    }
}
