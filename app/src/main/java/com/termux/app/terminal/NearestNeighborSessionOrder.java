package com.termux.app.terminal;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class NearestNeighborSessionOrder {

    public static final int NO_POSITION = -1;

    private NearestNeighborSessionOrder() {
    }

    @NonNull
    public static <SessionKey> List<SessionKey> orderCandidatesNearestFirst(
            @NonNull List<SessionKey> displayedOrder,
            int positionOfSessionThatLeaves,
            @NonNull List<SessionKey> candidates) {
        if (positionOfSessionThatLeaves < 0 || positionOfSessionThatLeaves >= displayedOrder.size()) {
            return new ArrayList<>(candidates);
        }

        Set<SessionKey> candidatesNotPlacedYet = new LinkedHashSet<>(candidates);
        List<SessionKey> nearestFirst = new ArrayList<>(candidates.size());

        for (int below = positionOfSessionThatLeaves + 1; below < displayedOrder.size(); below++) {
            SessionKey sessionKey = displayedOrder.get(below);
            if (candidatesNotPlacedYet.remove(sessionKey)) {
                nearestFirst.add(sessionKey);
            }
        }
        for (int above = positionOfSessionThatLeaves - 1; above >= 0; above--) {
            SessionKey sessionKey = displayedOrder.get(above);
            if (candidatesNotPlacedYet.remove(sessionKey)) {
                nearestFirst.add(sessionKey);
            }
        }

        nearestFirst.addAll(candidatesNotPlacedYet);
        return nearestFirst;
    }
}
