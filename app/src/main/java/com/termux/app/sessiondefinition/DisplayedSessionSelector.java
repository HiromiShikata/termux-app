package com.termux.app.sessiondefinition;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.shared.termux.settings.preferences.HiddenSessionNameMatcher;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public final class DisplayedSessionSelector {

    @NonNull
    public Set<String> selectDisplayedSessionNames(boolean activityVisible,
                                                   @Nullable String currentSessionName,
                                                   @NonNull Collection<String> allLiveSessionNames,
                                                   boolean hideHiddenSessions,
                                                   @NonNull Set<String> hiddenSessionNames) {
        return selectDisplayedSessionNames(activityVisible, currentSessionName, allLiveSessionNames,
            hideHiddenSessions, hiddenSessionNames, null);
    }

    @NonNull
    public Set<String> selectDisplayedSessionNames(boolean activityVisible,
                                                   @Nullable String currentSessionName,
                                                   @NonNull Collection<String> allLiveSessionNames,
                                                   boolean hideHiddenSessions,
                                                   @NonNull Set<String> hiddenSessionNames,
                                                   @Nullable Set<String> expandedProjectSessionNames) {
        if (!activityVisible) {
            return new LinkedHashSet<>();
        }
        return selectDisplayedSessionNamesRegardlessOfActivityVisibility(currentSessionName,
            allLiveSessionNames, hideHiddenSessions, hiddenSessionNames, expandedProjectSessionNames);
    }

    /**
     * The same selection without the activity-visibility gate, for the periodic background cycle. The
     * cycle keeps ticking while the app is backgrounded, and the gated selection returns an empty set in
     * that state, which makes every firing return at its empty-set guard without detecting a call-to-user
     * tag or reconnecting a dead session. The exclusions are unchanged: a session the owner hid stays out,
     * a session under a collapsed project stays out, and the current session is always kept.
     */
    @NonNull
    public Set<String> selectDisplayedSessionNamesRegardlessOfActivityVisibility(
            @Nullable String currentSessionName,
            @NonNull Collection<String> allLiveSessionNames,
            boolean hideHiddenSessions,
            @NonNull Set<String> hiddenSessionNames,
            @Nullable Set<String> expandedProjectSessionNames) {
        Set<String> displayedSessionNames = new LinkedHashSet<>();
        for (String sessionName : allLiveSessionNames) {
            if (sessionName == null) {
                continue;
            }
            if (sessionName.equals(currentSessionName)) {
                displayedSessionNames.add(sessionName);
                continue;
            }
            if (hideHiddenSessions && HiddenSessionNameMatcher.matchesAHiddenSession(sessionName, hiddenSessionNames)) {
                continue;
            }
            if (expandedProjectSessionNames != null
                && !expandedProjectSessionNames.contains(sessionName)) {
                continue;
            }
            displayedSessionNames.add(sessionName);
        }
        if (currentSessionName != null) {
            displayedSessionNames.add(currentSessionName);
        }
        return displayedSessionNames;
    }
}
