package com.termux.app.diagnostics;

public final class BackgroundCycleInterval {

    private final long mIntervalMillis;

    private final long mObservedAtMillis;

    private final long mScheduledIntervalMillis;

    private final boolean mActivityVisible;

    public BackgroundCycleInterval(long intervalMillis, long observedAtMillis,
                                   long scheduledIntervalMillis, boolean activityVisible) {
        mIntervalMillis = intervalMillis;
        mObservedAtMillis = observedAtMillis;
        mScheduledIntervalMillis = scheduledIntervalMillis;
        mActivityVisible = activityVisible;
    }

    public long getIntervalMillis() {
        return mIntervalMillis;
    }

    public long getObservedAtMillis() {
        return mObservedAtMillis;
    }

    public long getScheduledIntervalMillis() {
        return mScheduledIntervalMillis;
    }

    public boolean isActivityVisible() {
        return mActivityVisible;
    }
}
