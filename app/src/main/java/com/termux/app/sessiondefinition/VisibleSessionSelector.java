package com.termux.app.sessiondefinition;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class VisibleSessionSelector {

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
        if (currentSessionName != null) {
            visibleSessionNames.add(currentSessionName);
        }
        if (sessionListOpen) {
            for (String onScreenListSessionName : onScreenListSessionNames) {
                if (onScreenListSessionName == null) {
                    continue;
                }
                if (hiddenSessionNames.contains(onScreenListSessionName)) {
                    continue;
                }
                visibleSessionNames.add(onScreenListSessionName);
            }
        }
        return visibleSessionNames;
    }
}
