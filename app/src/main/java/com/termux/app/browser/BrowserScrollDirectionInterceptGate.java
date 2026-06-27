package com.termux.app.browser;

import android.view.MotionEvent;

public final class BrowserScrollDirectionInterceptGate {

    public static final int UPWARD_SCROLL_SLOP_PIXELS = 8;

    private boolean mHasDownPosition;
    private float mDownY;

    public boolean shouldDeclineInterception(int action, float currentY) {
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mHasDownPosition = true;
                mDownY = currentY;
                return false;
            case MotionEvent.ACTION_MOVE:
                return mHasDownPosition && isUpwardScroll(mDownY, currentY);
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mHasDownPosition = false;
                return false;
            default:
                return false;
        }
    }

    public static boolean isUpwardScroll(float downY, float currentY) {
        return currentY < downY - UPWARD_SCROLL_SLOP_PIXELS;
    }
}
