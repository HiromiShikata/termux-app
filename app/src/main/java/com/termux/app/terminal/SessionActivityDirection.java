package com.termux.app.terminal;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.Map;

public final class SessionActivityDirection {

    @NonNull
    private final SessionNewActivityTier tier;
    private final boolean activeAbove;
    private final boolean activeBelow;

    private SessionActivityDirection(@NonNull SessionNewActivityTier tier,
                                     boolean activeAbove, boolean activeBelow) {
        this.tier = tier;
        this.activeAbove = activeAbove;
        this.activeBelow = activeBelow;
    }

    @NonNull
    public SessionNewActivityTier getTier() {
        return tier;
    }

    public boolean hasActiveAbove() {
        return activeAbove;
    }

    public boolean hasActiveBelow() {
        return activeBelow;
    }

    @NonNull
    public static SessionActivityDirection compute(@NonNull List<Integer> orderedSessionIndexes,
                                                   int currentSessionIndex,
                                                   @NonNull Map<Integer, SessionNewActivityTier> tiersByIndex) {
        SessionNewActivityTier activeTier = globalActiveTier(orderedSessionIndexes, tiersByIndex);
        if (activeTier == SessionNewActivityTier.NONE) {
            return new SessionActivityDirection(SessionNewActivityTier.NONE, false, false);
        }
        int currentPosition = orderedSessionIndexes.indexOf(currentSessionIndex);
        boolean activeAbove = false;
        boolean activeBelow = false;
        for (int position = 0; position < orderedSessionIndexes.size(); position++) {
            if (position == currentPosition) {
                continue;
            }
            if (tierAt(orderedSessionIndexes, position, tiersByIndex) != activeTier) {
                continue;
            }
            if (currentPosition < 0 || position < currentPosition) {
                activeAbove = true;
            } else {
                activeBelow = true;
            }
        }
        return new SessionActivityDirection(activeTier, activeAbove, activeBelow);
    }

    @NonNull
    private static SessionNewActivityTier globalActiveTier(@NonNull List<Integer> orderedSessionIndexes,
                                                           @NonNull Map<Integer, SessionNewActivityTier> tiersByIndex) {
        for (int position = 0; position < orderedSessionIndexes.size(); position++) {
            if (tierAt(orderedSessionIndexes, position, tiersByIndex) == SessionNewActivityTier.RED) {
                return SessionNewActivityTier.RED;
            }
        }
        return SessionNewActivityTier.NONE;
    }

    @NonNull
    private static SessionNewActivityTier tierAt(@NonNull List<Integer> orderedSessionIndexes, int position,
                                                 @NonNull Map<Integer, SessionNewActivityTier> tiersByIndex) {
        SessionNewActivityTier tier = tiersByIndex.get(orderedSessionIndexes.get(position));
        return tier == null ? SessionNewActivityTier.NONE : tier;
    }
}
