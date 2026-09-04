package com.termux.app.browser;

import android.view.MotionEvent;

public final class BrowserScrollUpSuppressionGate {

    private boolean mGestureInProgress;
    private boolean mStartedWithChildScrollable;

    public boolean shouldSuppressRefresh(int action, boolean childCanScrollUp, boolean longPressUnlocked) {
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mGestureInProgress = true;
                mStartedWithChildScrollable = childCanScrollUp;
                return childCanScrollUp;
            case MotionEvent.ACTION_MOVE:
                if (!mGestureInProgress) return false;
                if (longPressUnlocked) return false;
                return mStartedWithChildScrollable;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mGestureInProgress = false;
                mStartedWithChildScrollable = false;
                return childCanScrollUp;
            default:
                return childCanScrollUp;
        }
    }
}
