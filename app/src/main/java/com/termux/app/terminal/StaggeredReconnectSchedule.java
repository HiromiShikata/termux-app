package com.termux.app.terminal;

public final class StaggeredReconnectSchedule {

    private final long intervalMillis;
    private final int concurrentWindow;

    public StaggeredReconnectSchedule(long intervalMillis, int concurrentWindow) {
        this.intervalMillis = Math.max(0L, intervalMillis);
        this.concurrentWindow = Math.max(1, concurrentWindow);
    }

    public long startDelayMillisForIndex(int reconnectIndex) {
        if (reconnectIndex < concurrentWindow) {
            return 0L;
        }
        long staggeredPosition = (reconnectIndex - concurrentWindow) / (long) concurrentWindow + 1L;
        return staggeredPosition * intervalMillis;
    }
}
