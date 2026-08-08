package com.termux.app.diagnostics;

import androidx.annotation.NonNull;

public final class MainThreadStallHotPath {

    private static final String APPLICATION_FRAME_PREFIX = "com.termux.";

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

    @NonNull
    public String getIdentifyingFrame() {
        String[] frames = mStackTrace.split("\n");
        for (String frame : frames) {
            if (frame.startsWith(APPLICATION_FRAME_PREFIX)) {
                return frame;
            }
        }
        return frames[0];
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
