package com.termux.app.diagnostics;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class ProcessConditionSnapshot {

    public static final ProcessConditionSnapshot NOT_RECORDED =
        new ProcessConditionSnapshot(false, null, 0L, 0L, 0, 0, 0, 0L, 0, 0,
            ScrollAnswerTotals.NONE);

    private final boolean mRecorded;

    @Nullable
    private final String mUnreadableReason;

    private final long mRecordedAtMillis;

    private final long mProcessUptimeMillis;

    private final int mMainLooperPendingMessageCount;

    private final int mSynchronizationBarrierCount;

    private final int mPeakPendingMessageCount;

    private final long mPeakObservedAtMillis;

    private final int mPeakScrollbarViewCount;

    private final int mKeptScrollWithoutDrawEpisodeCount;

    @NonNull
    private final ScrollAnswerTotals mScrollAnswerTotals;

    private ProcessConditionSnapshot(boolean recorded, @Nullable String unreadableReason,
                                     long recordedAtMillis, long processUptimeMillis,
                                     int mainLooperPendingMessageCount,
                                     int synchronizationBarrierCount,
                                     int peakPendingMessageCount, long peakObservedAtMillis,
                                     int peakScrollbarViewCount,
                                     int keptScrollWithoutDrawEpisodeCount,
                                     @NonNull ScrollAnswerTotals scrollAnswerTotals) {
        mRecorded = recorded;
        mUnreadableReason = unreadableReason;
        mRecordedAtMillis = recordedAtMillis;
        mProcessUptimeMillis = processUptimeMillis;
        mMainLooperPendingMessageCount = mainLooperPendingMessageCount;
        mSynchronizationBarrierCount = synchronizationBarrierCount;
        mPeakPendingMessageCount = peakPendingMessageCount;
        mPeakObservedAtMillis = peakObservedAtMillis;
        mPeakScrollbarViewCount = peakScrollbarViewCount;
        mKeptScrollWithoutDrawEpisodeCount = keptScrollWithoutDrawEpisodeCount;
        mScrollAnswerTotals = scrollAnswerTotals;
    }

    @NonNull
    public static ProcessConditionSnapshot recorded(long recordedAtMillis, long processUptimeMillis,
                                                    int mainLooperPendingMessageCount,
                                                    int synchronizationBarrierCount,
                                                    int peakPendingMessageCount,
                                                    long peakObservedAtMillis,
                                                    int peakScrollbarViewCount,
                                                    int keptScrollWithoutDrawEpisodeCount,
                                                    @NonNull ScrollAnswerTotals scrollAnswerTotals) {
        return new ProcessConditionSnapshot(true, null, recordedAtMillis, processUptimeMillis,
            mainLooperPendingMessageCount, synchronizationBarrierCount, peakPendingMessageCount,
            peakObservedAtMillis, peakScrollbarViewCount, keptScrollWithoutDrawEpisodeCount,
            scrollAnswerTotals);
    }

    @NonNull
    public static ProcessConditionSnapshot unreadable(@NonNull String reason) {
        return new ProcessConditionSnapshot(false, reason, 0L, 0L, 0, 0, 0, 0L, 0, 0,
            ScrollAnswerTotals.NONE);
    }

    public boolean isRecorded() {
        return mRecorded;
    }

    @Nullable
    public String getUnreadableReason() {
        return mUnreadableReason;
    }

    public long getRecordedAtMillis() {
        return mRecordedAtMillis;
    }

    public long getProcessUptimeMillis() {
        return mProcessUptimeMillis;
    }

    public int getMainLooperPendingMessageCount() {
        return mMainLooperPendingMessageCount;
    }

    public int getSynchronizationBarrierCount() {
        return mSynchronizationBarrierCount;
    }

    public int getPeakPendingMessageCount() {
        return mPeakPendingMessageCount;
    }

    public long getPeakObservedAtMillis() {
        return mPeakObservedAtMillis;
    }

    public int getPeakScrollbarViewCount() {
        return mPeakScrollbarViewCount;
    }

    public int getKeptScrollWithoutDrawEpisodeCount() {
        return mKeptScrollWithoutDrawEpisodeCount;
    }

    @NonNull
    public ScrollAnswerTotals getScrollAnswerTotals() {
        return mScrollAnswerTotals;
    }
}
