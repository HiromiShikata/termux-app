package com.termux.app.diagnostics;

import androidx.annotation.NonNull;

public final class MainThreadStallHotPath {

    private final String mStackTrace;

    private final long mStallCount;

    private final long mTotalBlockedMillis;

    private final long mMaxBlockedMillis;

    public MainThreadStallHotPath(@NonNull String stackTrace, long stallCount,
                                  long totalBlockedMillis, long maxBlockedMillis) {
        mStackTrace = stackTrace;
        mStallCount = stallCount;
        mTotalBlockedMillis = totalBlockedMillis;
        mMaxBlockedMillis = maxBlockedMillis;
    }

    @NonNull
    public String getStackTrace() {
        return mStackTrace;
    }

    public long getStallCount() {
        return mStallCount;
    }

    public long getTotalBlockedMillis() {
        return mTotalBlockedMillis;
    }

    public long getMaxBlockedMillis() {
        return mMaxBlockedMillis;
    }
}
