package com.termux.app.diagnostics;

import android.os.SystemClock;
import android.view.View;
import android.view.ViewTreeObserver;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class WindowDrawTimeObserver {

    @NonNull
    private final DrawTimeRecorder mRecorder;

    @Nullable
    private View mObservedDecorView;

    @Nullable
    private ViewTreeObserver.OnDrawListener mOnDrawListener;

    public WindowDrawTimeObserver(@NonNull DrawTimeRecorder recorder) {
        mRecorder = recorder;
    }

    public void observe(@NonNull View decorView) {
        stop();
        ViewTreeObserver.OnDrawListener onDrawListener =
            () -> mRecorder.record(decorView, SystemClock.elapsedRealtime());
        mObservedDecorView = decorView;
        mOnDrawListener = onDrawListener;
        decorView.getViewTreeObserver().addOnDrawListener(onDrawListener);
    }

    public void stop() {
        if (mObservedDecorView == null) {
            return;
        }
        mObservedDecorView.getViewTreeObserver().removeOnDrawListener(mOnDrawListener);
        mObservedDecorView = null;
        mOnDrawListener = null;
    }
}
