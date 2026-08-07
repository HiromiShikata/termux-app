package com.termux.app.diagnostics;

import androidx.annotation.NonNull;

public final class DiagnosticsSessionReconnectCost {

    private final long mReconnectCount;
    private final long mTotalElapsedMillis;
    private final long mMaxElapsedMillis;
    private final int mSessionsStillQueuedAtMaxElapsed;

    public DiagnosticsSessionReconnectCost(long reconnectCount, long totalElapsedMillis,
                                           long maxElapsedMillis, int sessionsStillQueuedAtMaxElapsed) {
        mReconnectCount = reconnectCount;
        mTotalElapsedMillis = totalElapsedMillis;
        mMaxElapsedMillis = maxElapsedMillis;
        mSessionsStillQueuedAtMaxElapsed = sessionsStillQueuedAtMaxElapsed;
    }

    @NonNull
    public static DiagnosticsSessionReconnectCost of(@NonNull SessionReconnectCostCounter counter) {
        return new DiagnosticsSessionReconnectCost(counter.getReconnectCount(),
            counter.getTotalElapsedMillis(), counter.getMaxElapsedMillis(),
            counter.getSessionsStillQueuedAtMaxElapsed());
    }

    public long getReconnectCount() {
        return mReconnectCount;
    }

    public long getTotalElapsedMillis() {
        return mTotalElapsedMillis;
    }

    public long getMaxElapsedMillis() {
        return mMaxElapsedMillis;
    }

    public int getSessionsStillQueuedAtMaxElapsed() {
        return mSessionsStillQueuedAtMaxElapsed;
    }
}
