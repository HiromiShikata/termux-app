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
        List<Set<String>> sessionNameBatches = sessionNameBatches(sessionNames, batchSize);
        List<Batch> batches = new ArrayList<>();
        for (int batchIndex = 0; batchIndex < sessionNameBatches.size(); batchIndex++) {
            batches.add(new Batch(batchIndex * batchIntervalMillis,
                sessionNameBatches.get(batchIndex)));
        }
        return batches;
    }

    @NonNull
    public static List<Set<String>> sessionNameBatches(@NonNull List<String> sessionNames,
                                                       int batchSize) {
        if (batchSize <= 0) {
            throw new IllegalArgumentException("batchSize must be positive but was " + batchSize);
        }
        if (sessionNames.isEmpty()) return Collections.emptyList();
        List<Set<String>> sessionNameBatches = new ArrayList<>();
        for (int start = 0; start < sessionNames.size(); start += batchSize) {
            int end = Math.min(start + batchSize, sessionNames.size());
            sessionNameBatches.add(new LinkedHashSet<>(sessionNames.subList(start, end)));
        }
        return sessionNameBatches;
    }
}
