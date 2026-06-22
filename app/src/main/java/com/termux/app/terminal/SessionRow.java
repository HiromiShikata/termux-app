package com.termux.app.terminal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SessionRow {

    static final float SESSION_NAME_RELATIVE_SIZE = 0.85f;

    static final String NEW_ACTIVITY_LABEL_PREFIX = "  ";

    private final int sessionIndex;
    private final String name;
    private final String resolvedTitle;
    private final String project;
    private final String story;
    private final SessionNewActivityTier tier;
    private final String activityAgeLabel;
    private final boolean disabled;
    private final boolean current;

    SessionRow(int sessionIndex, @NonNull String name, @NonNull String resolvedTitle,
               @NonNull String project, @NonNull String story, @NonNull SessionNewActivityTier tier,
               @NonNull String activityAgeLabel, boolean disabled, boolean current) {
        this.sessionIndex = sessionIndex;
        this.name = name;
        this.resolvedTitle = resolvedTitle;
        this.project = project;
        this.story = story;
        this.tier = tier;
        this.activityAgeLabel = activityAgeLabel;
        this.disabled = disabled;
        this.current = current;
    }

    public int getSessionIndex() {
        return sessionIndex;
    }

    @NonNull
    public String getName() {
        return name;
    }

    @NonNull
    public String getResolvedTitle() {
        return resolvedTitle;
    }

    @NonNull
    public String getProject() {
        return project;
    }

    @NonNull
    public String getStory() {
        return story;
    }

    @NonNull
    public SessionNewActivityTier getTier() {
        return tier;
    }

    public boolean isActivityMarked() {
        return tier != SessionNewActivityTier.NONE;
    }

    @NonNull
    public String getActivityAgeLabel() {
        return activityAgeLabel;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public boolean isCurrent() {
        return current;
    }

    @NonNull
    public static Map<Integer, SessionRow> project(@NonNull List<String> namesByIndex,
                                                   @NonNull List<String> titlesByIndex,
                                                   @NonNull List<String> projectsByIndex,
                                                   @NonNull List<String> storiesByIndex,
                                                   @NonNull Map<Integer, SessionNewActivityTier> tiersByIndex,
                                                   @NonNull Map<Integer, String> activityAgeLabelsByIndex,
                                                   @NonNull Set<Integer> disabledIndexes,
                                                   int currentSessionIndex) {
        Map<Integer, SessionRow> rowsByIndex = new LinkedHashMap<>();
        int sessionCount = namesByIndex.size();
        for (int sessionIndex = 0; sessionIndex < sessionCount; sessionIndex++) {
            SessionNewActivityTier tier = tiersByIndex.get(sessionIndex);
            if (tier == null) {
                tier = SessionNewActivityTier.NONE;
            }
            String activityAgeLabel = activityAgeLabelsByIndex.get(sessionIndex);
            rowsByIndex.put(sessionIndex, new SessionRow(
                sessionIndex,
                valueAt(namesByIndex, sessionIndex),
                valueAt(titlesByIndex, sessionIndex),
                valueAt(projectsByIndex, sessionIndex),
                valueAt(storiesByIndex, sessionIndex),
                tier,
                activityAgeLabel == null ? "" : activityAgeLabel,
                disabledIndexes.contains(sessionIndex),
                sessionIndex == currentSessionIndex));
        }
        return Collections.unmodifiableMap(rowsByIndex);
    }

    @NonNull
    static SessionRow rowOrEmpty(@Nullable Map<Integer, SessionRow> rowsByIndex, int sessionIndex) {
        if (rowsByIndex == null) {
            return emptyAt(sessionIndex);
        }
        SessionRow row = rowsByIndex.get(sessionIndex);
        return row == null ? emptyAt(sessionIndex) : row;
    }

    @NonNull
    private static SessionRow emptyAt(int sessionIndex) {
        return new SessionRow(sessionIndex, "", "", "", "", SessionNewActivityTier.NONE, "", false, false);
    }

    @NonNull
    private static String valueAt(@NonNull List<String> values, int index) {
        if (index < 0 || index >= values.size()) {
            return "";
        }
        String value = values.get(index);
        return value == null ? "" : value;
    }
}
