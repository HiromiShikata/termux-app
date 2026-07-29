package com.termux.app.terminal;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class StaggeredStatuslineRescanBatchPlanner {

    public static final class Batch {

        private final long delayMillis;

        private final Set<String> sessionNames;

        private Batch(long delayMillis, @NonNull Set<String> sessionNames) {
            this.delayMillis = delayMillis;
            this.sessionNames = sessionNames;
        }

        public long getDelayMillis() {
            return delayMillis;
        }

        @NonNull
        public Set<String> getSessionNames() {
            return sessionNames;
        }
    }

    private StaggeredStatuslineRescanBatchPlanner() {
    }

    @NonNull
    public static List<Batch> plan(@NonNull List<String> sessionNames, int batchSize,
                                   long batchIntervalMillis) {
        if (batchSize <= 0) {
            throw new IllegalArgumentException("batchSize must be positive but was " + batchSize);
        }
        if (sessionNames.isEmpty()) return Collections.emptyList();
        List<Batch> batches = new ArrayList<>();
        int batchIndex = 0;
        for (int start = 0; start < sessionNames.size(); start += batchSize) {
            int end = Math.min(start + batchSize, sessionNames.size());
            batches.add(new Batch(batchIndex * batchIntervalMillis,
                new LinkedHashSet<>(sessionNames.subList(start, end))));
            batchIndex++;
        }
        return batches;
    }
}
