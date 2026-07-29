package com.termux.app.sessiondefinition;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
        Set<String> displayedSessionNames = new LinkedHashSet<>();
        if (!activityVisible) {
            return displayedSessionNames;
        }
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
