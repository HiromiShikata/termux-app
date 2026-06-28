package com.termux.app.terminal;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.Map;

public final class InitialSessionSelection {

    private InitialSessionSelection() {
    }

    public static int selectInitialSessionIndex(@NonNull List<Integer> orderedSessionIndexes,
                                                @NonNull Map<Integer, Boolean> pendingCallToUserByIndex) {
        if (orderedSessionIndexes.isEmpty()) {
            return -1;
        }
        for (int sessionIndex : orderedSessionIndexes) {
            if (Boolean.TRUE.equals(pendingCallToUserByIndex.get(sessionIndex))) {
                return sessionIndex;
            }
        }
        return orderedSessionIndexes.get(0);
    }
}
