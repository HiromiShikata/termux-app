package com.termux.app.browser;

import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class MainThreadDebouncer implements BrowserTabHistoryPersistScheduler.Debouncer {

    private final Handler mHandler;

    private final long mDelayMs;

    @Nullable
    private Runnable mPending;

    public MainThreadDebouncer(@NonNull Handler handler, long delayMs) {
        mHandler = handler;
        mDelayMs = delayMs;
    }

    @Override
    public void schedule(@NonNull Runnable task) {
        cancel();
        mPending = task;
        mHandler.postDelayed(task, mDelayMs);
    }

    @Override
    public void cancel() {
        if (mPending != null) {
            mHandler.removeCallbacks(mPending);
            mPending = null;
        }
    }
}
