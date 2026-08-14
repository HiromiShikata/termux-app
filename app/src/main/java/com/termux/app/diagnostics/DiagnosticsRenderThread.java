package com.termux.app.diagnostics;

import androidx.annotation.NonNull;

public final class DiagnosticsRenderThread {

    public static final String THREAD_NAME = "RenderThread";

    public static final DiagnosticsRenderThread NOT_TAKEN =
        new DiagnosticsRenderThread(Reading.NOT_TAKEN, "", "", 0L, 0L);

    public enum Reading {
        NOT_TAKEN,
        READ_FAILED,
        THREAD_ABSENT,
        MEASURED
    }

    @NonNull
    private final Reading mReading;

    @NonNull
    private final String mReadFailureMessage;

    @NonNull
    private final String mSchedulerState;

    private final long mUserTimeMillis;

    private final long mSystemTimeMillis;

    private DiagnosticsRenderThread(@NonNull Reading reading, @NonNull String readFailureMessage,
                                    @NonNull String schedulerState, long userTimeMillis,
                                    long systemTimeMillis) {
        mReading = reading;
        mReadFailureMessage = readFailureMessage;
        mSchedulerState = schedulerState;
        mUserTimeMillis = userTimeMillis;
        mSystemTimeMillis = systemTimeMillis;
    }

    @NonNull
    public static DiagnosticsRenderThread readFailed(@NonNull String readFailureMessage) {
        return new DiagnosticsRenderThread(Reading.READ_FAILED, readFailureMessage, "", 0L, 0L);
    }

    @NonNull
    public static DiagnosticsRenderThread threadAbsent() {
        return new DiagnosticsRenderThread(Reading.THREAD_ABSENT, "", "", 0L, 0L);
    }

    @NonNull
    public static DiagnosticsRenderThread measured(@NonNull String schedulerState,
                                                   long userTimeMillis, long systemTimeMillis) {
        return new DiagnosticsRenderThread(Reading.MEASURED, "", schedulerState, userTimeMillis,
            systemTimeMillis);
    }

    @NonNull
    public Reading getReading() {
        return mReading;
    }

    @NonNull
    public String getReadFailureMessage() {
        if (mReading != Reading.READ_FAILED) {
            throw new IllegalStateException("no read failure was recorded for the render thread");
        }
        return mReadFailureMessage;
    }

    @NonNull
    public String getSchedulerState() {
        requireMeasured();
        return mSchedulerState;
    }

    public long getUserTimeMillis() {
        requireMeasured();
        return mUserTimeMillis;
    }

    public long getSystemTimeMillis() {
        requireMeasured();
        return mSystemTimeMillis;
    }

    public long getProcessorTimeMillis() {
        requireMeasured();
        return mUserTimeMillis + mSystemTimeMillis;
    }

    private void requireMeasured() {
        if (mReading != Reading.MEASURED) {
            throw new IllegalStateException("the render thread was not measured, so it has no "
                + "processor time to report");
        }
    }
}
