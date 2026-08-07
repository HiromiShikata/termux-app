package com.termux.app.diagnostics;

/**
 * Remembers when the current process started so the diagnostics report can state how far into a
 * process lifetime its process-lifetime counters were read.
 */
public final class ProcessUptimeTracker {

    private static long sProcessStartElapsedRealtimeMillis;

    private ProcessUptimeTracker() {
    }

    public static void recordProcessStart(long elapsedRealtimeMillis) {
        sProcessStartElapsedRealtimeMillis = elapsedRealtimeMillis;
    }

    public static long uptimeMillis(long elapsedRealtimeMillis) {
        long uptimeMillis = elapsedRealtimeMillis - sProcessStartElapsedRealtimeMillis;
        return uptimeMillis > 0 ? uptimeMillis : 0;
    }
}
