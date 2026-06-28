package com.termux.app.sessiondefinition;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

public final class SessionReconnectScheduler {

    static final long MILLISECONDS_PER_MINUTE = 60_000L;

    private final Handler mMainThreadHandler;
    private final Runnable mReconnectAction;
    private final Runnable mTickRunnable = this::onTick;

    private int mIntervalMinutes;
    private boolean mForeground;
    private boolean mScheduled;

    public SessionReconnectScheduler(@NonNull Runnable reconnectAction) {
        this(new Handler(Looper.getMainLooper()), reconnectAction);
    }

    public SessionReconnectScheduler(@NonNull Handler mainThreadHandler, @NonNull Runnable reconnectAction) {
        this.mMainThreadHandler = mainThreadHandler;
        this.mReconnectAction = reconnectAction;
    }

    public void onForeground(int intervalMinutes) {
        mForeground = true;
        mIntervalMinutes = intervalMinutes;
        schedule();
    }

    public void onBackground() {
        mForeground = false;
        stop();
    }

    boolean isScheduled() {
        return mScheduled;
    }

    private void onTick() {
        mScheduled = false;
        if (!shouldSchedule(mForeground, mIntervalMinutes)) {
            return;
        }
        mReconnectAction.run();
        schedule();
    }

    private void schedule() {
        if (mScheduled) {
            return;
        }
        if (!shouldSchedule(mForeground, mIntervalMinutes)) {
            return;
        }
        mScheduled = true;
        mMainThreadHandler.postDelayed(mTickRunnable, intervalMillis(mIntervalMinutes));
    }

    private void stop() {
        mScheduled = false;
        mMainThreadHandler.removeCallbacks(mTickRunnable);
    }

    static boolean shouldSchedule(boolean foreground, int intervalMinutes) {
        return foreground && intervalMinutes > 0;
    }

    static long intervalMillis(int intervalMinutes) {
        return (long) intervalMinutes * MILLISECONDS_PER_MINUTE;
    }
}
