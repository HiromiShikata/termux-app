package com.termux.app.terminal;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class NotifiedSessionNavigationCandidates {

    private NotifiedSessionNavigationCandidates() {
    }

    @NonNull
    public static List<Integer> restrictToNotifiedWhenAny(@NonNull List<Integer> navigableSessionIndexes,
                                                          @NonNull Set<Integer> notifiedSessionIndexes) {
        List<Integer> notifiedCandidates = new ArrayList<>();
        for (int sessionIndex : navigableSessionIndexes) {
            if (notifiedSessionIndexes.contains(sessionIndex)) {
                notifiedCandidates.add(sessionIndex);
            }
        }
        if (notifiedCandidates.isEmpty()) {
            return new ArrayList<>(navigableSessionIndexes);
        }
        return notifiedCandidates;
    }
}
