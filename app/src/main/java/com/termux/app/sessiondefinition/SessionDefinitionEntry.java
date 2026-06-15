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
    private final String tdpmConsoleUrl;
    private final String newIssueUrl;

    public SessionDefinitionEntry(String groupLabel, String entryLabel, List<String> urls) {
        this(groupLabel, entryLabel, urls, Collections.emptyMap());
    }

    public SessionDefinitionEntry(String groupLabel, String entryLabel, List<String> urls,
                                  Map<String, String> titlesByUrl) {
        this(groupLabel, entryLabel, urls, titlesByUrl, null);
    }

    public SessionDefinitionEntry(String groupLabel, String entryLabel, List<String> urls,
                                  Map<String, String> titlesByUrl, @Nullable String overviewUrl) {
        this(groupLabel, entryLabel, urls, titlesByUrl, overviewUrl, null);
    }

    public SessionDefinitionEntry(String groupLabel, String entryLabel, List<String> urls,
                                  Map<String, String> titlesByUrl, @Nullable String overviewUrl,
                                  @Nullable String tdpmConsoleUrl) {
        this(groupLabel, entryLabel, urls, titlesByUrl, overviewUrl, tdpmConsoleUrl, null);
    }

    public SessionDefinitionEntry(String groupLabel, String entryLabel, List<String> urls,
                                  Map<String, String> titlesByUrl, @Nullable String overviewUrl,
                                  @Nullable String tdpmConsoleUrl, @Nullable String newIssueUrl) {
        this.groupLabel = groupLabel;
        this.entryLabel = entryLabel;
        this.urls = Collections.unmodifiableList(new ArrayList<>(urls));
        this.titlesByUrl = Collections.unmodifiableMap(new HashMap<>(titlesByUrl));
        this.overviewUrl = overviewUrl;
        this.tdpmConsoleUrl = tdpmConsoleUrl;
        this.newIssueUrl = newIssueUrl;
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

    @Nullable
    public String getTdpmConsoleUrl() {
        return tdpmConsoleUrl;
    }

    @Nullable
    public String getNewIssueUrl() {
        return newIssueUrl;
    }

    public String getSessionName() {
        if (groupLabel == null || groupLabel.isEmpty()) {
            return entryLabel;
        }
        return groupLabel + "/" + entryLabel;
    }
}
