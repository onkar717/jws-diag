package org.jboss.jws.diag.logs.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LogMatch {

    private final long lineNumber;
    private final String text;

    public LogMatch(long lineNumber, String text) {
        this.lineNumber = lineNumber;
        this.text = text;
    }

    @JsonProperty("line")
    public long getLineNumber() { return lineNumber; }

    @JsonProperty("text")
    public String getText() { return text; }
}
