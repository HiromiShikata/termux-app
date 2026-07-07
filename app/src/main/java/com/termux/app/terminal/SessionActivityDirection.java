package com.termux.app.terminal;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SessionActivityDirection {

    @NonNull
    private final SessionNewActivityTier tier;
    private final int activeAboveCount;
    private final int activeBelowCount;

    private SessionActivityDirection(@NonNull SessionNewActivityTier tier,
                                     int activeAboveCount, int activeBelowCount) {
        this.tier = tier;
        this.activeAboveCount = activeAboveCount;
        this.activeBelowCount = activeBelowCount;
    }

    @NonNull
    public SessionNewActivityTier getTier() {
        return tier;
    }

    public boolean hasActiveAbove() {
        return activeAboveCount > 0;
    }

    public boolean hasActiveBelow() {
        return activeBelowCount > 0;
    }

    public int getActiveAboveCount() {
        return activeAboveCount;
    }

    public int getActiveBelowCount() {
        return activeBelowCount;
    }

    @NonNull
    public static SessionActivityDirection compute(@NonNull List<Integer> orderedSessionIndexes,
                                                   int currentSessionIndex,
                                                   @NonNull List<String> sessionNamesByIndex,
                                                   @NonNull Set<String> callingSessionNames) {
        return ofCallingSessionSplit(CallingSessionNavigator.split(
            orderedSessionIndexes, sessionNamesByIndex, callingSessionNames, currentSessionIndex));
    }

    @NonNull
    public static SessionActivityDirection ofCallingSessionSplit(@NonNull CallingSessionSplit split) {
        SessionNewActivityTier tier = split.getTotalCount() > 0
            ? SessionNewActivityTier.RED : SessionNewActivityTier.NONE;
        return new SessionActivityDirection(tier, split.getAboveCount(), split.getBelowCount());
    }

    @NonNull
    public static SessionActivityDirection compute(@NonNull List<Integer> orderedSessionIndexes,
                                                   int currentSessionIndex,
                                                   @NonNull Map<Integer, SessionNewActivityTier> tiersByIndex) {
        SessionNewActivityTier activeTier = globalActiveTier(orderedSessionIndexes, tiersByIndex);
        if (activeTier == SessionNewActivityTier.NONE) {
            return new SessionActivityDirection(SessionNewActivityTier.NONE, 0, 0);
        }
        int currentPosition = orderedSessionIndexes.indexOf(currentSessionIndex);
        int activeAboveCount = 0;
        int activeBelowCount = 0;
        for (int position = 0; position < orderedSessionIndexes.size(); position++) {
            if (position == currentPosition) {
                continue;
            }
            if (tierAt(orderedSessionIndexes, position, tiersByIndex) != activeTier) {
                continue;
            }
            if (currentPosition < 0 || position < currentPosition) {
                activeAboveCount++;
            } else {
                activeBelowCount++;
            }
        }
        return new SessionActivityDirection(activeTier, activeAboveCount, activeBelowCount);
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
