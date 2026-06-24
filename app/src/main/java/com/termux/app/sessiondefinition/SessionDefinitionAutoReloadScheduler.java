package com.termux.app.sessiondefinition;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

public final class SessionDefinitionAutoReloadScheduler {

    static final long MILLISECONDS_PER_MINUTE = 60_000L;

    private final Handler mMainThreadHandler;
    private final Runnable mReloadAction;
    private final Runnable mTickRunnable = this::onTick;

    private int mIntervalMinutes;
    private boolean mForeground;
    private boolean mScheduled;

    public SessionDefinitionAutoReloadScheduler(@NonNull Runnable reloadAction) {
        this(new Handler(Looper.getMainLooper()), reloadAction);
    }

    public SessionDefinitionAutoReloadScheduler(@NonNull Handler mainThreadHandler, @NonNull Runnable reloadAction) {
        this.mMainThreadHandler = mainThreadHandler;
        this.mReloadAction = reloadAction;
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
        mReloadAction.run();
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
