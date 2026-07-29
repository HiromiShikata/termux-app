package com.termux.app.sessiondefinition;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class VisibleSessionSelector {

    @NonNull
    public Set<String> selectVisibleSessionNames(boolean activityVisible,
                                                 @Nullable String currentSessionName,
                                                 boolean sessionListOpen,
                                                 @NonNull List<String> onScreenListSessionNames) {
        return selectVisibleSessionNames(activityVisible, currentSessionName, sessionListOpen,
            onScreenListSessionNames, Collections.emptySet());
    }

    /**
     * The displayed session is always selected, so nothing here can make a displayed session less
     * fresh. A hidden session is never selected, because a hidden session holds no runtime resource
     * to scan or reconnect, even while the show-hidden toggle renders it as an on-screen row.
     */
    @NonNull
    public Set<String> selectVisibleSessionNames(boolean activityVisible,
                                                 @Nullable String currentSessionName,
                                                 boolean sessionListOpen,
                                                 @NonNull List<String> onScreenListSessionNames,
                                                 @NonNull Set<String> hiddenSessionNames) {
        Set<String> visibleSessionNames = new LinkedHashSet<>();
        if (!activityVisible) {
            return visibleSessionNames;
        }
        if (currentSessionName != null && !hiddenSessionNames.contains(currentSessionName)) {
            visibleSessionNames.add(currentSessionName);
        }
        if (sessionListOpen) {
            for (String onScreenListSessionName : onScreenListSessionNames) {
                if (onScreenListSessionName == null
                        || hiddenSessionNames.contains(onScreenListSessionName)) {
                    continue;
                }
                visibleSessionNames.add(onScreenListSessionName);
            }
        }
        return visibleSessionNames;
    }
}
