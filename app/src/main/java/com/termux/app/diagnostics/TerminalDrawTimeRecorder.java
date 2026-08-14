package com.termux.app.diagnostics;

import androidx.annotation.NonNull;

public final class TerminalDrawTimeRecorder {

    private boolean mHasDrawn;

    private long mLastDrawElapsedRealtimeMillis;

    public synchronized void record(long elapsedRealtimeMillis) {
        mHasDrawn = true;
        mLastDrawElapsedRealtimeMillis = elapsedRealtimeMillis;
    }

    @NonNull
    public synchronized DiagnosticsTerminalDrawTime snapshot(long elapsedRealtimeMillis) {
        if (!mHasDrawn) return DiagnosticsTerminalDrawTime.NEVER_DRAWN;
        return DiagnosticsTerminalDrawTime.drawnMillisAgo(
            elapsedRealtimeMillis - mLastDrawElapsedRealtimeMillis);
    }
}
