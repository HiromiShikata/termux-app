package com.termux.app.browser;

import androidx.annotation.NonNull;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

public final class BrowserRenderProcessCrashTracker {

    public static final int MAX_CRASHES_WITHIN_WINDOW = 2;

    public static final long CRASH_WINDOW_MILLIS = 30_000L;

    private final Map<String, Deque<Long>> mCrashTimestampsByTabId = new HashMap<>();

    public boolean recordCrashAndCheckLooping(@NonNull String tabId, long nowMillis) {
        Deque<Long> timestamps = mCrashTimestampsByTabId.get(tabId);
        if (timestamps == null) {
            timestamps = new ArrayDeque<>();
            mCrashTimestampsByTabId.put(tabId, timestamps);
        }
        timestamps.addLast(nowMillis);
        while (!timestamps.isEmpty() && nowMillis - timestamps.peekFirst() > CRASH_WINDOW_MILLIS) {
            timestamps.removeFirst();
        }
        return timestamps.size() > MAX_CRASHES_WITHIN_WINDOW;
    }

    public void forgetTab(@NonNull String tabId) {
        mCrashTimestampsByTabId.remove(tabId);
    }
}
