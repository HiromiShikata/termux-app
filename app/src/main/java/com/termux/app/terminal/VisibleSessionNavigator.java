package com.termux.app.terminal;

import androidx.annotation.NonNull;

import java.util.List;

public final class VisibleSessionNavigator {

    private VisibleSessionNavigator() {
    }

    public static int nextSessionIndex(@NonNull List<Integer> orderedSessionIndexes,
                                       @NonNull List<Integer> navigableSessionIndexes,
                                       int currentSessionIndex,
                                       boolean forward) {
        if (navigableSessionIndexes.isEmpty()) {
            return -1;
        }
        int navigablePosition = navigableSessionIndexes.indexOf(currentSessionIndex);
        if (navigablePosition >= 0) {
            int size = navigableSessionIndexes.size();
            int nextPosition = forward
                ? (navigablePosition + 1) % size
                : (navigablePosition - 1 + size) % size;
            return navigableSessionIndexes.get(nextPosition);
        }
        return nextNavigableInDisplayOrder(
            orderedSessionIndexes, navigableSessionIndexes, currentSessionIndex, forward);
    }

    private static int nextNavigableInDisplayOrder(@NonNull List<Integer> orderedSessionIndexes,
                                                   @NonNull List<Integer> navigableSessionIndexes,
                                                   int currentSessionIndex,
                                                   boolean forward) {
        int orderedPosition = orderedSessionIndexes.indexOf(currentSessionIndex);
        if (orderedPosition < 0) {
            return numericNearestNavigableIndex(navigableSessionIndexes, currentSessionIndex, forward);
        }
        int orderedSize = orderedSessionIndexes.size();
        for (int step = 1; step <= orderedSize; step++) {
            int candidatePosition = forward
                ? (orderedPosition + step) % orderedSize
                : (orderedPosition - step + orderedSize) % orderedSize;
            int candidateSessionIndex = orderedSessionIndexes.get(candidatePosition);
            if (navigableSessionIndexes.contains(candidateSessionIndex)) {
                return candidateSessionIndex;
            }
        }
        return numericNearestNavigableIndex(navigableSessionIndexes, currentSessionIndex, forward);
    }

    private static int numericNearestNavigableIndex(@NonNull List<Integer> navigableSessionIndexes,
                                                    int currentSessionIndex,
                                                    boolean forward) {
        Integer wrapCandidate = null;
        Integer directionalCandidate = null;
        for (int sessionIndex : navigableSessionIndexes) {
            if (forward) {
                if (sessionIndex > currentSessionIndex
                        && (directionalCandidate == null || sessionIndex < directionalCandidate)) {
                    directionalCandidate = sessionIndex;
                }
                if (wrapCandidate == null || sessionIndex < wrapCandidate) {
                    wrapCandidate = sessionIndex;
                }
            } else {
                if (sessionIndex < currentSessionIndex
                        && (directionalCandidate == null || sessionIndex > directionalCandidate)) {
                    directionalCandidate = sessionIndex;
                }
                if (wrapCandidate == null || sessionIndex > wrapCandidate) {
                    wrapCandidate = sessionIndex;
                }
            }
        }
        return directionalCandidate != null ? directionalCandidate : wrapCandidate;
    }
}
