package com.termux.app.terminal;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

public final class SessionOutputProgressTracker {

    private final Map<String, Long> mLastScrolledLineCountByName = new HashMap<>();

    public boolean hasNewOutput(@NonNull String sessionName, long scrolledLineCount) {
        Long lastScrolledLineCount = mLastScrolledLineCountByName.get(sessionName);
        mLastScrolledLineCountByName.put(sessionName, scrolledLineCount);
        if (lastScrolledLineCount == null) {
            return scrolledLineCount > 0L;
        }
        return scrolledLineCount > lastScrolledLineCount;
    }

    public void forget(@NonNull String sessionName) {
        mLastScrolledLineCountByName.remove(sessionName);
    }
}
