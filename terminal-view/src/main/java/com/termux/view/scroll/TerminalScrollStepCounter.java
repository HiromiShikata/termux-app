package com.termux.view.scroll;

import java.util.EnumMap;
import java.util.Map;

public final class TerminalScrollStepCounter {

    private final Map<TerminalScrollEvent, Integer> mStepCountsByDestination =
        new EnumMap<>(TerminalScrollEvent.class);

    private final Map<TerminalScrollEvent, Long> mLastStepMillisByDestination =
        new EnumMap<>(TerminalScrollEvent.class);

    public synchronized void record(TerminalScrollEvent destination, long steppedAtMillis) {
        Integer stepCount = mStepCountsByDestination.get(destination);
        mStepCountsByDestination.put(destination, stepCount == null ? 1 : stepCount + 1);
        mLastStepMillisByDestination.put(destination, steppedAtMillis);
    }

    public synchronized int getStepCount(TerminalScrollEvent destination) {
        Integer stepCount = mStepCountsByDestination.get(destination);
        return stepCount == null ? 0 : stepCount;
    }

    public synchronized long getLastStepAtMillis(TerminalScrollEvent destination) {
        Long lastStepMillis = mLastStepMillisByDestination.get(destination);
        if (lastStepMillis == null) {
            throw new IllegalStateException("no scroll step has gone to " + destination);
        }
        return lastStepMillis;
    }

    public synchronized int getTotalStepCount() {
        int totalStepCount = 0;
        for (Integer stepCount : mStepCountsByDestination.values()) {
            totalStepCount += stepCount;
        }
        return totalStepCount;
    }
}
