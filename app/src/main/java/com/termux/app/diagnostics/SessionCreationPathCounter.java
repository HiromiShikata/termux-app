package com.termux.app.diagnostics;

import androidx.annotation.NonNull;

import java.util.EnumMap;
import java.util.Map;

public final class SessionCreationPathCounter {

    @NonNull
    private final Map<SessionCreationPath, Integer> mCreationCountsByPath =
        new EnumMap<>(SessionCreationPath.class);

    public synchronized void record(@NonNull SessionCreationPath path) {
        Integer creationCount = mCreationCountsByPath.get(path);
        mCreationCountsByPath.put(path, creationCount == null ? 1 : creationCount + 1);
    }

    public synchronized int getCreationCount(@NonNull SessionCreationPath path) {
        Integer creationCount = mCreationCountsByPath.get(path);
        return creationCount == null ? 0 : creationCount;
    }

    public synchronized int getTotalCreationCount() {
        int total = 0;
        for (Integer creationCount : mCreationCountsByPath.values()) {
            total += creationCount;
        }
        return total;
    }
}
