package com.termux.app.terminal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class SessionHierarchyRow {

    public enum Type {
        PROJECT_HEADER,
        STORY_HEADER,
        SESSION
    }

    private final Type type;
    private final String label;
    private final int sessionIndex;
    private final String overviewUrl;
    private final String tdpmConsoleUrl;
    private final String newIssueUrl;

    private SessionHierarchyRow(@NonNull Type type, @Nullable String label, int sessionIndex,
                               @Nullable String overviewUrl, @Nullable String tdpmConsoleUrl,
                               @Nullable String newIssueUrl) {
        this.type = type;
        this.label = label;
        this.sessionIndex = sessionIndex;
        this.overviewUrl = overviewUrl;
        this.tdpmConsoleUrl = tdpmConsoleUrl;
        this.newIssueUrl = newIssueUrl;
    }

    @NonNull
    public static SessionHierarchyRow projectHeader(@NonNull String label) {
        return projectHeader(label, null, null, null);
    }

    @NonNull
    public static SessionHierarchyRow projectHeader(@NonNull String label, @Nullable String overviewUrl,
                                                    @Nullable String tdpmConsoleUrl,
                                                    @Nullable String newIssueUrl) {
        return new SessionHierarchyRow(Type.PROJECT_HEADER, label, -1, overviewUrl, tdpmConsoleUrl, newIssueUrl);
    }

    @NonNull
    public static SessionHierarchyRow storyHeader(@NonNull String label) {
        return new SessionHierarchyRow(Type.STORY_HEADER, label, -1, null, null, null);
    }

    @NonNull
    public static SessionHierarchyRow session(int sessionIndex) {
        return new SessionHierarchyRow(Type.SESSION, null, sessionIndex, null, null, null);
    }

    @NonNull
    public Type getType() {
        return type;
    }

    public boolean isHeader() {
        return type != Type.SESSION;
    }

    @Nullable
    public String getLabel() {
        return label;
    }

    public int getSessionIndex() {
        return sessionIndex;
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
}
