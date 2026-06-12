package com.termux.app.sessiondefinition;

public final class SessionDefinitionGroupReference {

    private final String label;
    private final String url;

    public SessionDefinitionGroupReference(String label, String url) {
        this.label = label;
        this.url = url;
    }

    public String getLabel() {
        return label;
    }

    public String getUrl() {
        return url;
    }
}
