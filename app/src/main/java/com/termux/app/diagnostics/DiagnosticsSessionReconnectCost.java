package com.termux.app.diagnostics;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class DiagnosticsSessionReconnectCost {

    private final long mReconnectCount;
    private final long mTotalElapsedMillis;
    private final long mMaxElapsedMillis;
    private final int mSessionsStillQueuedAtMaxElapsed;
    private final List<DiagnosticsSessionReconnectCostByReason> mCostsByReason;

    public DiagnosticsSessionReconnectCost(long reconnectCount, long totalElapsedMillis,
                                           long maxElapsedMillis, int sessionsStillQueuedAtMaxElapsed,
                                           @NonNull List<DiagnosticsSessionReconnectCostByReason> costsByReason) {
        mReconnectCount = reconnectCount;
        mTotalElapsedMillis = totalElapsedMillis;
        mMaxElapsedMillis = maxElapsedMillis;
        mSessionsStillQueuedAtMaxElapsed = sessionsStillQueuedAtMaxElapsed;
        mCostsByReason = Collections.unmodifiableList(new ArrayList<>(costsByReason));
    }

    @NonNull
    public static DiagnosticsSessionReconnectCost of(@NonNull SessionReconnectCostCounter counter) {
        return new DiagnosticsSessionReconnectCost(counter.getReconnectCount(),
            counter.getTotalElapsedMillis(), counter.getMaxElapsedMillis(),
            counter.getSessionsStillQueuedAtMaxElapsed(), counter.getCostsByReason());
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

    @NonNull
    public List<DiagnosticsSessionReconnectCostByReason> getCostsByReason() {
        return mCostsByReason;
    }
}
