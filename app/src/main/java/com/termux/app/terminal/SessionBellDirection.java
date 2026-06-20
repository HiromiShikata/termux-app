package com.termux.app.terminal;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.Set;

public final class SessionBellDirection {

    private final boolean bellAbove;
    private final boolean bellBelow;

    private SessionBellDirection(boolean bellAbove, boolean bellBelow) {
        this.bellAbove = bellAbove;
        this.bellBelow = bellBelow;
    }

    public boolean hasBellAbove() {
        return bellAbove;
    }

    public boolean hasBellBelow() {
        return bellBelow;
    }

    @NonNull
    public static SessionBellDirection compute(@NonNull List<Integer> orderedSessionIndexes,
                                               int currentSessionIndex,
                                               @NonNull Set<Integer> unseenBellSessionIndexes) {
        int currentPosition = orderedSessionIndexes.indexOf(currentSessionIndex);
        if (currentPosition < 0) {
            return new SessionBellDirection(false, false);
        }
        boolean bellAbove = false;
        boolean bellBelow = false;
        for (int position = 0; position < orderedSessionIndexes.size(); position++) {
            if (position == currentPosition) {
                continue;
            }
            if (!unseenBellSessionIndexes.contains(orderedSessionIndexes.get(position))) {
                continue;
            }
            if (position < currentPosition) {
                bellAbove = true;
            } else {
                bellBelow = true;
            }
        }
        return new SessionBellDirection(bellAbove, bellBelow);
    }
}
