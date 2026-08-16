package com.termux.app.diagnostics;

import androidx.annotation.NonNull;

import java.util.Map;
import java.util.WeakHashMap;

public final class DrawTimeRecorder {

    @NonNull
    private final Map<Object, Long> mLastDrawElapsedRealtimeMillisByDrawnBy = new WeakHashMap<>();

    public synchronized void record(@NonNull Object drawnBy, long elapsedRealtimeMillis) {
        mLastDrawElapsedRealtimeMillisByDrawnBy.put(drawnBy, elapsedRealtimeMillis);
    }

    @NonNull
    public synchronized DiagnosticsDrawTime snapshot(@NonNull Object drawnBy,
                                                     long elapsedRealtimeMillis) {
        Long lastDrawElapsedRealtimeMillis = mLastDrawElapsedRealtimeMillisByDrawnBy.get(drawnBy);
        if (lastDrawElapsedRealtimeMillis == null) return DiagnosticsDrawTime.NEVER_DRAWN;
        return DiagnosticsDrawTime.drawnMillisAgo(
            elapsedRealtimeMillis - lastDrawElapsedRealtimeMillis);
    }

    public synchronized long getLastDrawElapsedRealtimeMillis(@NonNull Object drawnBy) {
        Long lastDrawElapsedRealtimeMillis = mLastDrawElapsedRealtimeMillisByDrawnBy.get(drawnBy);
        if (lastDrawElapsedRealtimeMillis == null) {
            throw new IllegalStateException("nothing has been drawn by " + drawnBy);
        }
        return lastDrawElapsedRealtimeMillis;
    }
}
