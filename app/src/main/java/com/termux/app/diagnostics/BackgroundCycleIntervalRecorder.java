package com.termux.app.diagnostics;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BackgroundCycleIntervalRecorder {

    public static final int DEFAULT_MAX_RETAINED_INTERVALS = 8;

    private final int mMaxRetainedIntervals;

    private final List<BackgroundCycleInterval> mLongestIntervals = new ArrayList<>();

    private long mCycleCount;

    private boolean mHasPreviousCycle;

    private long mPreviousCycleAtMillis;

    public BackgroundCycleIntervalRecorder() {
        this(DEFAULT_MAX_RETAINED_INTERVALS);
    }

    public BackgroundCycleIntervalRecorder(int maxRetainedIntervals) {
        mMaxRetainedIntervals = maxRetainedIntervals;
    }

    public synchronized void recordCycle(long cycleAtMillis, long scheduledIntervalMillis,
                                         boolean activityVisible) {
        mCycleCount++;
        if (mHasPreviousCycle) {
            retainAmongTheLongest(new BackgroundCycleInterval(cycleAtMillis - mPreviousCycleAtMillis,
                cycleAtMillis, scheduledIntervalMillis, activityVisible));
        }
        mHasPreviousCycle = true;
        mPreviousCycleAtMillis = cycleAtMillis;
    }

    private void retainAmongTheLongest(@NonNull BackgroundCycleInterval interval) {
        mLongestIntervals.add(interval);
        Collections.sort(mLongestIntervals,
            (left, right) -> Long.compare(right.getIntervalMillis(), left.getIntervalMillis()));
        while (mLongestIntervals.size() > mMaxRetainedIntervals) {
            mLongestIntervals.remove(mLongestIntervals.size() - 1);
        }
    }

    public synchronized long getCycleCount() {
        return mCycleCount;
    }

    @NonNull
    public synchronized List<BackgroundCycleInterval> getLongestIntervals() {
        return Collections.unmodifiableList(new ArrayList<>(mLongestIntervals));
    }
}
