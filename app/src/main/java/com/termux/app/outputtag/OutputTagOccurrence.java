package com.termux.app.outputtag;

public final class OutputTagOccurrence {

    private final String value;

    private final String deduplicationKey;

    public OutputTagOccurrence(String value, String deduplicationKey) {
        this.value = value;
        this.deduplicationKey = deduplicationKey;
    }

    public String getValue() {
        return value;
    }

    public String getDeduplicationKey() {
        return deduplicationKey;
    }
}
