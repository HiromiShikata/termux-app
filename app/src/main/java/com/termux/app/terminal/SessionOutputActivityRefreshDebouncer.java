package com.termux.app.terminal;

public class SessionOutputActivityRefreshDebouncer {

    private final long mMinimumIntervalMillis;

    private boolean mHasRefreshed;

    private long mLastRefreshTimeMillis;

    public SessionOutputActivityRefreshDebouncer(long minimumIntervalMillis) {
        this.mMinimumIntervalMillis = minimumIntervalMillis;
    }

    public boolean shouldRefresh(long nowMillis) {
        if (!mHasRefreshed) {
            mHasRefreshed = true;
            mLastRefreshTimeMillis = nowMillis;
            return true;
        }
        if (nowMillis - mLastRefreshTimeMillis < mMinimumIntervalMillis) {
            return false;
        }
        mLastRefreshTimeMillis = nowMillis;
        return true;
    }
}
