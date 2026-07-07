package com.termux.app.terminal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.Set;

public final class CallingSessionNavigator {

    private CallingSessionNavigator() {
    }

    public static int topmostCallingSessionIndex(@NonNull List<Integer> orderedSessionIndexes,
                                                 @NonNull List<String> sessionNamesByIndex,
                                                 @NonNull Set<String> callingSessionNames) {
        if (callingSessionNames.isEmpty()) {
            return -1;
        }
        for (int orderedSessionIndex : orderedSessionIndexes) {
            String sessionName = sessionNameOrNull(sessionNamesByIndex, orderedSessionIndex);
            if (sessionName != null && callingSessionNames.contains(sessionName)) {
                return orderedSessionIndex;
            }
        }
        return -1;
    }

    public static int callingSessionCount(@NonNull List<Integer> orderedSessionIndexes,
                                          @NonNull List<String> sessionNamesByIndex,
                                          @NonNull Set<String> callingSessionNames) {
        if (callingSessionNames.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (int orderedSessionIndex : orderedSessionIndexes) {
            String sessionName = sessionNameOrNull(sessionNamesByIndex, orderedSessionIndex);
            if (sessionName != null && callingSessionNames.contains(sessionName)) {
                count++;
            }
        }
        return count;
    }

    @Nullable
    private static String sessionNameOrNull(@NonNull List<String> sessionNamesByIndex, int sessionIndex) {
        if (sessionIndex < 0 || sessionIndex >= sessionNamesByIndex.size()) {
            return null;
        }
        return sessionNamesByIndex.get(sessionIndex);
    }
}
