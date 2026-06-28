package com.termux.app.terminal;

import androidx.annotation.NonNull;

import java.util.List;

public final class SessionRemovalNeighborSelector {

    public static final int NO_NEIGHBOR = -1;

    private SessionRemovalNeighborSelector() {
    }

    public static int selectNeighborSessionIndex(
            @NonNull List<Integer> orderedVisibleSessionIndexesBeforeRemoval,
            int removedSessionIndex) {
        int removedPosition = orderedVisibleSessionIndexesBeforeRemoval.indexOf(removedSessionIndex);
        if (removedPosition < 0) {
            return firstOtherSessionIndex(orderedVisibleSessionIndexesBeforeRemoval, removedSessionIndex);
        }

        int lastPosition = orderedVisibleSessionIndexesBeforeRemoval.size() - 1;
        if (removedPosition < lastPosition) {
            return orderedVisibleSessionIndexesBeforeRemoval.get(removedPosition + 1);
        }
        if (removedPosition > 0) {
            return orderedVisibleSessionIndexesBeforeRemoval.get(removedPosition - 1);
        }
        return NO_NEIGHBOR;
    }

    private static int firstOtherSessionIndex(
            @NonNull List<Integer> orderedVisibleSessionIndexesBeforeRemoval,
            int removedSessionIndex) {
        for (int sessionIndex : orderedVisibleSessionIndexesBeforeRemoval) {
            if (sessionIndex != removedSessionIndex) {
                return sessionIndex;
            }
        }
        return NO_NEIGHBOR;
    }
}
