package com.termux.app.sessiondefinition;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class VisibleSessionSelector {

    /**
     * The on-screen session-list rows include hidden sessions whenever the owner turns the
     * show-hidden toggle on, so the hidden names are subtracted here. Without that subtraction a
     * hidden session enters the general reconnect scheduler and the statusline re-parse that both
     * read this set, which is the one path on which a hidden session still consumes work. The
     * current session is never subtracted: it is on screen by definition and must stay current.
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
