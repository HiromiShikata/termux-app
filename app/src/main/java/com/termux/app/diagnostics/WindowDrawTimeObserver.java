package com.termux.app.diagnostics;

import android.os.SystemClock;
import android.view.View;
import android.view.ViewTreeObserver;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class WindowDrawTimeObserver {

    @NonNull
    private final DrawTimeRecorder mRecorder;

    @NonNull
    private final ViewTreeObserver.OnDrawListener mOnDrawListener = new ViewTreeObserver.OnDrawListener() {
        @Override
        public void onDraw() {
            mRecorder.record(SystemClock.elapsedRealtime());
        }
    };

    @Nullable
    private View mObservedDecorView;

    public WindowDrawTimeObserver(@NonNull DrawTimeRecorder recorder) {
        mRecorder = recorder;
    }

    public void observe(@NonNull View decorView) {
        stop();
        mObservedDecorView = decorView;
        decorView.getViewTreeObserver().addOnDrawListener(mOnDrawListener);
    }

    public void stop() {
        if (mObservedDecorView == null) {
            return;
        }
        mObservedDecorView.getViewTreeObserver().removeOnDrawListener(mOnDrawListener);
        mObservedDecorView = null;
    }
}
