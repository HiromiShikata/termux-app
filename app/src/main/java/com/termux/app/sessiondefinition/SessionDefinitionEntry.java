package com.termux.app.sessiondefinition;

import java.util.Collections;
import java.util.List;

public final class SessionDefinitionEntry {

    private final String groupLabel;
    private final String entryLabel;
    private final List<String> urls;

    public SessionDefinitionEntry(String groupLabel, String entryLabel, List<String> urls) {
        this.groupLabel = groupLabel;
        this.entryLabel = entryLabel;
        this.urls = Collections.unmodifiableList(urls);
    }

    public String getGroupLabel() {
        return groupLabel;
    }

    public String getEntryLabel() {
        return entryLabel;
    }

    public List<String> getUrls() {
        return urls;
    }

    public String getSessionName() {
        if (groupLabel == null || groupLabel.isEmpty()) {
            return entryLabel;
        }
        return groupLabel + "/" + entryLabel;
    }
}
