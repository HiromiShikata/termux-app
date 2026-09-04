package com.termux.app.browser;

import android.view.MotionEvent;

public final class BrowserScrollLongPressGate {

    public static final long LONG_PRESS_THRESHOLD_MS = 2000L;
    public static final float MOVEMENT_CANCEL_SLOP_PIXELS = 20f;

    private boolean mGestureInProgress;
    private long mDownTimeMs;
    private float mDownY;
    private boolean mLongPressUnlocked;
    private boolean mMovedBeforeThreshold;

    public boolean isLongPressUnlocked(int action, float y, long eventTimeMs) {
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mGestureInProgress = true;
                mDownTimeMs = eventTimeMs;
                mDownY = y;
                mLongPressUnlocked = false;
                mMovedBeforeThreshold = false;
                return false;
            case MotionEvent.ACTION_MOVE:
                if (!mGestureInProgress) return false;
                if (mLongPressUnlocked) return true;
                if (mMovedBeforeThreshold) return false;
                if (Math.abs(y - mDownY) > MOVEMENT_CANCEL_SLOP_PIXELS) {
                    mMovedBeforeThreshold = true;
                    return false;
                }
                if (eventTimeMs - mDownTimeMs >= LONG_PRESS_THRESHOLD_MS) {
                    mLongPressUnlocked = true;
                    return true;
                }
                return false;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mGestureInProgress = false;
                mLongPressUnlocked = false;
                mMovedBeforeThreshold = false;
                return false;
            default:
                return mLongPressUnlocked;
        }
    }
}
