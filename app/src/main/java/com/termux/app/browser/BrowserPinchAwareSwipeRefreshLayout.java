package com.termux.app.browser;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public final class BrowserPinchAwareSwipeRefreshLayout extends SwipeRefreshLayout {

    private final BrowserPinchGestureInterceptGate mPinchGestureInterceptGate =
        new BrowserPinchGestureInterceptGate();

    public BrowserPinchAwareSwipeRefreshLayout(@NonNull Context context) {
        super(context);
    }

    public BrowserPinchAwareSwipeRefreshLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        boolean declineInterception = mPinchGestureInterceptGate.shouldDeclineInterception(
            event.getActionMasked(), event.getPointerCount());
        if (declineInterception) {
            return false;
        }
        return super.onInterceptTouchEvent(event);
    }
}
