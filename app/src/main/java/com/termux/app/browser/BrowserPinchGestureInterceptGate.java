package com.termux.app.browser;

import android.view.MotionEvent;

public final class BrowserPinchGestureInterceptGate {

    private boolean mMultiTouchGestureInProgress;

    public boolean shouldDeclineInterception(int action, int pointerCount) {
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mMultiTouchGestureInProgress = pointerCount > 1;
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                mMultiTouchGestureInProgress = true;
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mMultiTouchGestureInProgress = false;
                break;
            default:
                if (pointerCount > 1) {
                    mMultiTouchGestureInProgress = true;
                }
                break;
        }
        return mMultiTouchGestureInProgress || pointerCount > 1;
    }

    public boolean isMultiTouchGestureInProgress() {
        return mMultiTouchGestureInProgress;
    }
}
