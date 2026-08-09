package com.termux.app.diagnostics;

import androidx.annotation.NonNull;

import com.termux.app.sessiondefinition.SessionReconnectReason;

public final class DiagnosticsSessionReconnectCostByReason {

    private final SessionReconnectReason mReason;
    private final long mReconnectCount;
    private final long mTotalElapsedMillis;
    private final long mMaxElapsedMillis;

    public DiagnosticsSessionReconnectCostByReason(@NonNull SessionReconnectReason reason,
                                                   long reconnectCount, long totalElapsedMillis,
                                                   long maxElapsedMillis) {
        mReason = reason;
        mReconnectCount = reconnectCount;
        mTotalElapsedMillis = totalElapsedMillis;
        mMaxElapsedMillis = maxElapsedMillis;
    }

    @NonNull
    public SessionReconnectReason getReason() {
        return mReason;
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
}
