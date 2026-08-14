package com.termux.app.diagnostics;

import androidx.annotation.NonNull;

public final class DrawTimeRecorder {

    private boolean mHasDrawn;

    private long mLastDrawElapsedRealtimeMillis;

    public synchronized void record(long elapsedRealtimeMillis) {
        mHasDrawn = true;
        mLastDrawElapsedRealtimeMillis = elapsedRealtimeMillis;
    }

    @NonNull
    public synchronized DiagnosticsDrawTime snapshot(long elapsedRealtimeMillis) {
        if (!mHasDrawn) return DiagnosticsDrawTime.NEVER_DRAWN;
        return DiagnosticsDrawTime.drawnMillisAgo(
            elapsedRealtimeMillis - mLastDrawElapsedRealtimeMillis);
    }
}
