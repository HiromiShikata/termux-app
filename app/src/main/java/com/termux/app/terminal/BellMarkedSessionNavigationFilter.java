package com.termux.app.terminal;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class BellMarkedSessionNavigationFilter {

    private BellMarkedSessionNavigationFilter() {
    }

    @NonNull
    public static List<Integer> bellRestrictedNavigableIndexes(@NonNull List<Integer> navigableSessionIndexes,
                                                               @NonNull Set<Integer> markedSessionIndexes) {
        List<Integer> markedNavigableSessionIndexes = new ArrayList<>(navigableSessionIndexes.size());
        for (int sessionIndex : navigableSessionIndexes) {
            if (markedSessionIndexes.contains(sessionIndex)) {
                markedNavigableSessionIndexes.add(sessionIndex);
            }
        }
        if (markedNavigableSessionIndexes.isEmpty()) {
            return navigableSessionIndexes;
        }
        return markedNavigableSessionIndexes;
    }
}
