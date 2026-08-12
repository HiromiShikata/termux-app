package com.termux.app.diagnostics;

public final class MainThreadStallSampleSchedule {

    private final long mStallThresholdMillis;

    private final long mPollIntervalMillis;

    public MainThreadStallSampleSchedule(long stallThresholdMillis, long pollIntervalMillis) {
        mStallThresholdMillis = stallThresholdMillis;
        mPollIntervalMillis = pollIntervalMillis;
    }

    public long sleepMillisAfterAttemptAt(long elapsedMillis) {
        long remainingUntilThreshold = mStallThresholdMillis - elapsedMillis;
        if (remainingUntilThreshold > 0) {
            return remainingUntilThreshold;
        }
        return mPollIntervalMillis;
    }
}
