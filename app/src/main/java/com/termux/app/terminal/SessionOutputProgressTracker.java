package com.termux.app.terminal;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

public final class SessionOutputProgressTracker {

    private final Map<String, Long> mLastBytesProcessedByName = new HashMap<>();

    public boolean hasNewOutput(@NonNull String sessionName, long totalBytesProcessed) {
        Long lastBytesProcessed = mLastBytesProcessedByName.get(sessionName);
        mLastBytesProcessedByName.put(sessionName, totalBytesProcessed);
        if (lastBytesProcessed == null) {
            return totalBytesProcessed > 0L;
        }
        return totalBytesProcessed > lastBytesProcessed;
    }

    public void forget(@NonNull String sessionName) {
        mLastBytesProcessedByName.remove(sessionName);
    }
}
