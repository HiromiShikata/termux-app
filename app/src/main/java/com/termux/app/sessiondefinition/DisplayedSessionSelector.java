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
        Set<String> displayedSessionNames = new LinkedHashSet<>();
        if (!activityVisible) {
            return displayedSessionNames;
        }
        for (String sessionName : allLiveSessionNames) {
            if (sessionName == null) {
                continue;
            }
            if (hideHiddenSessions && hiddenSessionNames.contains(sessionName)
                && !sessionName.equals(currentSessionName)) {
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
