package com.termux.app.browser;

import android.view.MotionEvent;

public final class BrowserScrollUpSuppressionGate {

    private boolean mGestureInProgress;

    public boolean shouldSuppressRefresh(int action, boolean childCanScrollUp) {
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mGestureInProgress = true;
                return childCanScrollUp;
            case MotionEvent.ACTION_MOVE:
                return mGestureInProgress && childCanScrollUp;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mGestureInProgress = false;
                return childCanScrollUp;
            default:
                return childCanScrollUp;
        }
    }
}
