package com.termux.app.terminal;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class NotifiedSessionNavigationCandidates {

    private NotifiedSessionNavigationCandidates() {
    }

    @NonNull
    public static List<Integer> restrictToActiveTier(@NonNull List<Integer> navigableSessionIndexes,
                                                     @NonNull Map<Integer, SessionNewActivityTier> tiersByIndex) {
        return restrictToActiveTier(navigableSessionIndexes, tiersByIndex, -1);
    }

    @NonNull
    public static List<Integer> restrictToActiveTier(@NonNull List<Integer> navigableSessionIndexes,
                                                     @NonNull Map<Integer, SessionNewActivityTier> tiersByIndex,
                                                     int currentSessionIndex) {
        List<Integer> redCandidates = candidatesForTier(
            navigableSessionIndexes, tiersByIndex, SessionNewActivityTier.RED);
        if (!redCandidates.isEmpty() && !restrictingTrapsCurrentSession(redCandidates, currentSessionIndex)) {
            return redCandidates;
        }
        return new ArrayList<>(navigableSessionIndexes);
    }

    private static boolean restrictingTrapsCurrentSession(@NonNull List<Integer> redCandidates,
                                                          int currentSessionIndex) {
        return redCandidates.size() == 1 && redCandidates.get(0) == currentSessionIndex;
    }

    @NonNull
    private static List<Integer> candidatesForTier(@NonNull List<Integer> navigableSessionIndexes,
                                                   @NonNull Map<Integer, SessionNewActivityTier> tiersByIndex,
                                                   @NonNull SessionNewActivityTier tier) {
        List<Integer> candidates = new ArrayList<>();
        for (int sessionIndex : navigableSessionIndexes) {
            if (tiersByIndex.get(sessionIndex) == tier) {
                candidates.add(sessionIndex);
            }
        }
        return candidates;
    }
}
