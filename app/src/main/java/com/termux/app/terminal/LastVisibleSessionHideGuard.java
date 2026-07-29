package com.termux.app.terminal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.app.sessiondefinition.HiddenSessionNameMatcher;

import java.util.List;
import java.util.Set;

public final class LastVisibleSessionHideGuard {

    private LastVisibleSessionHideGuard() {
    }

    public static boolean hidingLeavesAVisibleSession(@Nullable String sessionNameBeingHidden,
                                                      @NonNull List<String> liveSessionNames,
                                                      @NonNull Set<String> hiddenSessionNames) {
        for (String liveSessionName : liveSessionNames) {
            if (liveSessionName == null || liveSessionName.isEmpty()) continue;
            if (liveSessionName.equals(sessionNameBeingHidden)) continue;
            if (HiddenSessionNameMatcher.matchesAHiddenSession(liveSessionName, hiddenSessionNames)) continue;
            return true;
        }
        return false;
    }
}
