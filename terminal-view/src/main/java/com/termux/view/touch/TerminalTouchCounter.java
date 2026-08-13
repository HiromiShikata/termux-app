package com.termux.view.touch;

import java.util.EnumMap;
import java.util.Map;

public final class TerminalTouchCounter {

    private final Map<TerminalTouchKind, Integer> mTouchCountsByKind =
        new EnumMap<>(TerminalTouchKind.class);

    private final Map<TerminalTouchKind, Long> mLastTouchMillisByKind =
        new EnumMap<>(TerminalTouchKind.class);

    public synchronized void record(TerminalTouchKind kind, long receivedAtMillis) {
        Integer touchCount = mTouchCountsByKind.get(kind);
        mTouchCountsByKind.put(kind, touchCount == null ? 1 : touchCount + 1);
        mLastTouchMillisByKind.put(kind, receivedAtMillis);
    }

    public synchronized int getTouchCount(TerminalTouchKind kind) {
        Integer touchCount = mTouchCountsByKind.get(kind);
        return touchCount == null ? 0 : touchCount;
    }

    public synchronized long getLastTouchAtMillis(TerminalTouchKind kind) {
        Long lastTouchMillis = mLastTouchMillisByKind.get(kind);
        if (lastTouchMillis == null) {
            throw new IllegalStateException("the terminal view has received no touch of kind " + kind);
        }
        return lastTouchMillis;
    }

    public synchronized int getTotalTouchCount() {
        int totalTouchCount = 0;
        for (Integer touchCount : mTouchCountsByKind.values()) {
            totalTouchCount += touchCount;
        }
        return totalTouchCount;
    }
}
