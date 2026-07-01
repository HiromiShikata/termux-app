package com.termux.app.terminal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

public final class NextVisibleSessionAfterKillSelector {

    private NextVisibleSessionAfterKillSelector() {
    }

    @Nullable
    public static String selectNextVisibleSessionName(
            @NonNull List<String> orderedVisibleSessionNamesBeforeKill,
            @NonNull String killedSessionName) {
        int killedPosition = orderedVisibleSessionNamesBeforeKill.indexOf(killedSessionName);
        if (killedPosition < 0) {
            return firstOtherSessionName(orderedVisibleSessionNamesBeforeKill, killedSessionName);
        }

        int lastPosition = orderedVisibleSessionNamesBeforeKill.size() - 1;
        if (killedPosition < lastPosition) {
            return orderedVisibleSessionNamesBeforeKill.get(killedPosition + 1);
        }
        if (killedPosition > 0) {
            return orderedVisibleSessionNamesBeforeKill.get(killedPosition - 1);
        }
        return null;
    }

    @Nullable
    private static String firstOtherSessionName(
            @NonNull List<String> orderedVisibleSessionNamesBeforeKill,
            @NonNull String killedSessionName) {
        for (String sessionName : orderedVisibleSessionNamesBeforeKill) {
            if (!killedSessionName.equals(sessionName)) {
                return sessionName;
            }
        }
        return null;
    }
}
