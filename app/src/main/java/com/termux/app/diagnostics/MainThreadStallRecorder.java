package com.termux.app.diagnostics;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class MainThreadStallRecorder {

    public static final String STACK_TRACE_NOT_SAMPLED = "not sampled";

    private static final int MAX_RECORDED_FRAMES = 16;

    private final long mStallThresholdMillis;

    private boolean mHeartbeatOutstanding;
    private long mHeartbeatPostedAtMillis;
    private String mOutstandingStackTrace = STACK_TRACE_NOT_SAMPLED;

    private long mStallCount;
    private long mMaxStallMillis;
    private String mMaxStallStackTrace = "";

    public MainThreadStallRecorder(long stallThresholdMillis) {
        mStallThresholdMillis = stallThresholdMillis;
    }

    public synchronized void heartbeatPosted(long postedAtMillis) {
        mHeartbeatOutstanding = true;
        mHeartbeatPostedAtMillis = postedAtMillis;
        mOutstandingStackTrace = STACK_TRACE_NOT_SAMPLED;
    }

    public synchronized void sampleWhileOutstanding(long sampledAtMillis,
                                                    @Nullable StackTraceElement[] mainThreadStackTrace) {
        if (!mHeartbeatOutstanding
                || sampledAtMillis - mHeartbeatPostedAtMillis < mStallThresholdMillis) {
            return;
        }
        mOutstandingStackTrace = formatStackTrace(mainThreadStackTrace);
    }

    public synchronized void heartbeatRan(long ranAtMillis) {
        if (!mHeartbeatOutstanding) {
            return;
        }
        mHeartbeatOutstanding = false;
        long stallMillis = ranAtMillis - mHeartbeatPostedAtMillis;
        if (stallMillis < mStallThresholdMillis) {
            return;
        }
        mStallCount++;
        if (stallMillis > mMaxStallMillis) {
            mMaxStallMillis = stallMillis;
            mMaxStallStackTrace = mOutstandingStackTrace;
        }
    }

    public synchronized long getStallCount() {
        return mStallCount;
    }

    public synchronized long getMaxStallMillis() {
        return mMaxStallMillis;
    }

    @NonNull
    public synchronized String getMaxStallStackTrace() {
        return mMaxStallStackTrace;
    }

    public long getStallThresholdMillis() {
        return mStallThresholdMillis;
    }

    @NonNull
    private static String formatStackTrace(@Nullable StackTraceElement[] stackTrace) {
        if (stackTrace == null || stackTrace.length == 0) {
            return STACK_TRACE_NOT_SAMPLED;
        }
        StringBuilder formatted = new StringBuilder();
        int frameCount = Math.min(stackTrace.length, MAX_RECORDED_FRAMES);
        for (int frameIndex = 0; frameIndex < frameCount; frameIndex++) {
            if (frameIndex > 0) {
                formatted.append('\n');
            }
            formatted.append(stackTrace[frameIndex].toString());
        }
        return formatted.toString();
    }
}
