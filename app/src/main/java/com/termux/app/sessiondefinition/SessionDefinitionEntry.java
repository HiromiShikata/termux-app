package com.termux.app.sessiondefinition;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SessionDefinitionEntry {

    private final String groupLabel;
    private final String entryLabel;
    private final List<String> urls;
    private final Map<String, String> titlesByUrl;
    private final String overviewUrl;

    public SessionDefinitionEntry(String groupLabel, String entryLabel, List<String> urls) {
        this(groupLabel, entryLabel, urls, Collections.emptyMap());
    }

    public SessionDefinitionEntry(String groupLabel, String entryLabel, List<String> urls,
                                  Map<String, String> titlesByUrl) {
        this(groupLabel, entryLabel, urls, titlesByUrl, null);
    }

    public SessionDefinitionEntry(String groupLabel, String entryLabel, List<String> urls,
                                  Map<String, String> titlesByUrl, @Nullable String overviewUrl) {
        this.groupLabel = groupLabel;
        this.entryLabel = entryLabel;
        this.urls = Collections.unmodifiableList(new ArrayList<>(urls));
        this.titlesByUrl = Collections.unmodifiableMap(new HashMap<>(titlesByUrl));
        this.overviewUrl = overviewUrl;
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

    @Nullable
    public String getTitleForUrl(String url) {
        return titlesByUrl.get(url);
    }

    @Nullable
    public String getOverviewUrl() {
        return overviewUrl;
    }

    public String getSessionName() {
        if (groupLabel == null || groupLabel.isEmpty()) {
            return entryLabel;
        }
        return groupLabel + "/" + entryLabel;
    }
}
