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
        return split(orderedSessionIndexes, sessionNamesByIndex, callingSessionNames, -1).getTotalCount();
    }

    @NonNull
    public static CallingSessionSplit split(@NonNull List<Integer> orderedSessionIndexes,
                                            @NonNull List<String> sessionNamesByIndex,
                                            @NonNull Set<String> callingSessionNames,
                                            int currentSessionIndex) {
        if (callingSessionNames.isEmpty()) {
            return new CallingSessionSplit(0, 0, false);
        }
        int currentPosition = orderedSessionIndexes.indexOf(currentSessionIndex);
        int aboveCount = 0;
        int belowCount = 0;
        boolean currentCalling = false;
        for (int position = 0; position < orderedSessionIndexes.size(); position++) {
            String sessionName = sessionNameOrNull(sessionNamesByIndex, orderedSessionIndexes.get(position));
            if (sessionName == null || !callingSessionNames.contains(sessionName)) {
                continue;
            }
            if (currentPosition >= 0 && position == currentPosition) {
                currentCalling = true;
            } else if (currentPosition < 0 || position < currentPosition) {
                aboveCount++;
            } else {
                belowCount++;
            }
        }
        return new CallingSessionSplit(aboveCount, belowCount, currentCalling);
    }

    @Nullable
    private static String sessionNameOrNull(@NonNull List<String> sessionNamesByIndex, int sessionIndex) {
        if (sessionIndex < 0 || sessionIndex >= sessionNamesByIndex.size()) {
            return null;
        }
        return sessionNamesByIndex.get(sessionIndex);
    }
}
