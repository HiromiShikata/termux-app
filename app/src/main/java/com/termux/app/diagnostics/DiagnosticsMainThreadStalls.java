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

    private final long mStackSampleAttemptCount;
    private final long mEmptyStackSampleCount;

    @NonNull
    private final DiagnosticsRenderThread mRenderThread;

    @NonNull
    private final DiagnosticsRenderThread mMaxStallRenderThread;

    public DiagnosticsMainThreadStalls(long thresholdMillis, long stallCount, long maxStallMillis,
                                       @NonNull String maxStallStackTrace,
                                       @NonNull List<MainThreadStallHotPath> hotPaths,
                                       long stackSampleAttemptCount, long emptyStackSampleCount) {
        this(thresholdMillis, stallCount, maxStallMillis, maxStallStackTrace, hotPaths,
            stackSampleAttemptCount, emptyStackSampleCount, DiagnosticsRenderThread.NOT_TAKEN,
            DiagnosticsRenderThread.NOT_TAKEN);
    }

    private DiagnosticsMainThreadStalls(long thresholdMillis, long stallCount, long maxStallMillis,
                                        @NonNull String maxStallStackTrace,
                                        @NonNull List<MainThreadStallHotPath> hotPaths,
                                        long stackSampleAttemptCount, long emptyStackSampleCount,
                                        @NonNull DiagnosticsRenderThread renderThread,
                                        @NonNull DiagnosticsRenderThread maxStallRenderThread) {
        mThresholdMillis = thresholdMillis;
        mStallCount = stallCount;
        mMaxStallMillis = maxStallMillis;
        mMaxStallStackTrace = maxStallStackTrace;
        mHotPaths = hotPaths;
        mStackSampleAttemptCount = stackSampleAttemptCount;
        mEmptyStackSampleCount = emptyStackSampleCount;
        mRenderThread = renderThread;
        mMaxStallRenderThread = maxStallRenderThread;
    }

    @NonNull
    public DiagnosticsMainThreadStalls withRenderThread(
        @NonNull DiagnosticsRenderThread renderThread) {
        return new DiagnosticsMainThreadStalls(mThresholdMillis, mStallCount, mMaxStallMillis,
            mMaxStallStackTrace, mHotPaths, mStackSampleAttemptCount, mEmptyStackSampleCount,
            renderThread, mMaxStallRenderThread);
    }

    @NonNull
    public DiagnosticsMainThreadStalls withMaxStallRenderThread(
        @NonNull DiagnosticsRenderThread maxStallRenderThread) {
        return new DiagnosticsMainThreadStalls(mThresholdMillis, mStallCount, mMaxStallMillis,
            mMaxStallStackTrace, mHotPaths, mStackSampleAttemptCount, mEmptyStackSampleCount,
            mRenderThread, maxStallRenderThread);
    }

    @NonNull
    public DiagnosticsRenderThread getRenderThread() {
        return mRenderThread;
    }

    @NonNull
    public DiagnosticsRenderThread getMaxStallRenderThread() {
        return mMaxStallRenderThread;
    }

    @NonNull
    public static DiagnosticsMainThreadStalls of(@NonNull MainThreadStallRecorder recorder) {
        return new DiagnosticsMainThreadStalls(recorder.getStallThresholdMillis(),
            recorder.getStallCount(), recorder.getMaxStallMillis(), recorder.getMaxStallStackTrace(),
            recorder.getHotPathsByTotalBlockedMillis(),
            recorder.getStackSampleAttemptCount(), recorder.getEmptyStackSampleCount())
            .withMaxStallRenderThread(recorder.getMaxStallRenderThread());
    }

    public long getStackSampleAttemptCount() {
        return mStackSampleAttemptCount;
    }

    public long getEmptyStackSampleCount() {
        return mEmptyStackSampleCount;
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
