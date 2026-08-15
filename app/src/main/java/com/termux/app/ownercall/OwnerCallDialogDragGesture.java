package com.termux.app.ownercall;

import androidx.annotation.Nullable;

public final class OwnerCallDialogDragGesture {

    private final int mTouchSlopPixels;

    private boolean mTracking = false;
    private boolean mDragging = false;
    private float mDownX = 0f;
    private float mDownY = 0f;
    private float mLastX = 0f;
    private float mLastY = 0f;

    public OwnerCallDialogDragGesture(int touchSlopPixels) {
        mTouchSlopPixels = Math.max(0, touchSlopPixels);
    }

    public void onTouchDown(float x, float y) {
        mTracking = true;
        mDragging = false;
        mDownX = x;
        mDownY = y;
        mLastX = x;
        mLastY = y;
    }

    @Nullable
    public OwnerCallDialogDragStep onTouchMoved(float x, float y) {
        if (!mTracking) {
            return null;
        }
        if (!mDragging) {
            if (Math.abs(x - mDownX) < mTouchSlopPixels && Math.abs(y - mDownY) < mTouchSlopPixels) {
                return null;
            }
            mDragging = true;
            mLastX = x;
            mLastY = y;
        }
        OwnerCallDialogDragStep step =
            new OwnerCallDialogDragStep((int) (x - mLastX), (int) (y - mLastY));
        mLastX = x;
        mLastY = y;
        return step;
    }

    public boolean onTouchFinished() {
        boolean wasDragging = mDragging;
        mTracking = false;
        mDragging = false;
        return wasDragging;
    }
}
