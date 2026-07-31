package com.termux.app.diagnostics;

import androidx.annotation.NonNull;

public final class DiagnosticsMainThreadStalls {

    private final long mThresholdMillis;
    private final long mStallCount;
    private final long mMaxStallMillis;

    @NonNull
    private final String mMaxStallStackTrace;

    public DiagnosticsMainThreadStalls(long thresholdMillis, long stallCount, long maxStallMillis,
                                       @NonNull String maxStallStackTrace) {
        mThresholdMillis = thresholdMillis;
        mStallCount = stallCount;
        mMaxStallMillis = maxStallMillis;
        mMaxStallStackTrace = maxStallStackTrace;
    }

    @NonNull
    public static DiagnosticsMainThreadStalls of(@NonNull MainThreadStallRecorder recorder) {
        return new DiagnosticsMainThreadStalls(recorder.getStallThresholdMillis(),
            recorder.getStallCount(), recorder.getMaxStallMillis(), recorder.getMaxStallStackTrace());
    }

    public long getThresholdMillis() {
        return mThresholdMillis;
    }

    public long getStallCount() {
        return mStallCount;
    }

    public long getMaxStallMillis() {
        return mMaxStallMillis;
    }

    @NonNull
    public String getMaxStallStackTrace() {
        return mMaxStallStackTrace;
    }
}
