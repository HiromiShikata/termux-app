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

    private final BrowserScrollDirectionInterceptGate mScrollDirectionInterceptGate =
        new BrowserScrollDirectionInterceptGate();

    private final BrowserScrollLongPressGate mLongPressGate =
        new BrowserScrollLongPressGate();

    private final BrowserScrollUpSuppressionGate mScrollUpSuppressionGate =
        new BrowserScrollUpSuppressionGate();

    public BrowserPinchAwareSwipeRefreshLayout(@NonNull Context context) {
        super(context);
    }

    public BrowserPinchAwareSwipeRefreshLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        boolean declinePinch = mPinchGestureInterceptGate.shouldDeclineInterception(
            action, event.getPointerCount());
        boolean declineUpwardScroll = mScrollDirectionInterceptGate.shouldDeclineInterception(
            action, event.getY());
        boolean longPressUnlocked = mLongPressGate.isLongPressUnlocked(
            action, event.getY(), event.getEventTime());
        boolean suppressWhileChildCanScrollUp = mScrollUpSuppressionGate.shouldSuppressRefresh(
            action, canChildScrollUp(), longPressUnlocked);
        if (declinePinch || declineUpwardScroll || suppressWhileChildCanScrollUp) {
            return false;
        }
        return super.onInterceptTouchEvent(event);
    }
}
