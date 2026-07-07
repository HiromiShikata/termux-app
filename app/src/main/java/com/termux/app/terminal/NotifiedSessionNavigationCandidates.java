package com.termux.app.terminal;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class NotifiedSessionNavigationCandidates {

    private NotifiedSessionNavigationCandidates() {
    }

    @NonNull
    public static List<Integer> restrictToCallingSessions(@NonNull List<Integer> orderedSessionIndexes,
                                                          @NonNull List<Integer> navigableSessionIndexes,
                                                          @NonNull List<String> sessionNamesByIndex,
                                                          @NonNull Set<String> callingSessionNames,
                                                          int currentSessionIndex) {
        List<Integer> callingCandidates = callingCandidatesInDisplayOrder(
            orderedSessionIndexes, sessionNamesByIndex, callingSessionNames);
        if (!callingCandidates.isEmpty() && !restrictingTrapsCurrentSession(callingCandidates, currentSessionIndex)) {
            return callingCandidates;
        }
        return new ArrayList<>(navigableSessionIndexes);
    }

    @NonNull
    private static List<Integer> callingCandidatesInDisplayOrder(@NonNull List<Integer> orderedSessionIndexes,
                                                                 @NonNull List<String> sessionNamesByIndex,
                                                                 @NonNull Set<String> callingSessionNames) {
        List<Integer> candidates = new ArrayList<>();
        if (callingSessionNames.isEmpty()) {
            return candidates;
        }
        for (int sessionIndex : orderedSessionIndexes) {
            if (sessionIndex < 0 || sessionIndex >= sessionNamesByIndex.size()) {
                continue;
            }
            String sessionName = sessionNamesByIndex.get(sessionIndex);
            if (sessionName != null && callingSessionNames.contains(sessionName)) {
                candidates.add(sessionIndex);
            }
        }
        return candidates;
    }

    private static boolean restrictingTrapsCurrentSession(@NonNull List<Integer> callingCandidates,
                                                          int currentSessionIndex) {
        return callingCandidates.size() == 1 && callingCandidates.get(0) == currentSessionIndex;
    }
}
