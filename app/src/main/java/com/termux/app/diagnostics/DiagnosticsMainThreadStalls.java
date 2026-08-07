package com.termux.app.diagnostics;

import androidx.annotation.NonNull;

import java.util.List;

public final class DiagnosticsMainThreadStalls {

    private final long mThresholdMillis;
    private final long mStallCount;
    private final long mMaxStallMillis;

    @NonNull
    private final String mMaxStallStackTrace;

    @NonNull
    private final List<MainThreadStallHotPath> mHotPaths;

    public DiagnosticsMainThreadStalls(long thresholdMillis, long stallCount, long maxStallMillis,
                                       @NonNull String maxStallStackTrace,
                                       @NonNull List<MainThreadStallHotPath> hotPaths) {
        mThresholdMillis = thresholdMillis;
        mStallCount = stallCount;
        mMaxStallMillis = maxStallMillis;
        mMaxStallStackTrace = maxStallStackTrace;
        mHotPaths = hotPaths;
    }

    @NonNull
    public static DiagnosticsMainThreadStalls of(@NonNull MainThreadStallRecorder recorder) {
        return new DiagnosticsMainThreadStalls(recorder.getStallThresholdMillis(),
            recorder.getStallCount(), recorder.getMaxStallMillis(), recorder.getMaxStallStackTrace(),
            recorder.getHotPathsByTotalBlockedMillis());
    }

    @NonNull
    public List<MainThreadStallHotPath> getHotPaths() {
        return mHotPaths;
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
