package com.termux.app.browser;

import android.app.Activity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class BrowserPinchAwareSwipeRefreshLayoutTest {

    private BrowserPinchAwareSwipeRefreshLayout newLayout() {
        return newLayout(false);
    }

    private BrowserPinchAwareSwipeRefreshLayout newLayout(boolean childCanScrollUp) {
        Activity activity = Robolectric.buildActivity(Activity.class).create().get();
        BrowserPinchAwareSwipeRefreshLayout layout = new BrowserPinchAwareSwipeRefreshLayout(activity);
        layout.addView(new View(activity), new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        layout.setOnChildScrollUpCallback((parent, child) -> childCanScrollUp);
        layout.measure(
            View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY));
        layout.layout(0, 0, 1000, 1000);
        return layout;
    }

    private MotionEvent verticalEvent(int action, float y) {
        long now = 100L;
        MotionEvent.PointerProperties pointerProperties = new MotionEvent.PointerProperties();
        pointerProperties.id = 0;
        MotionEvent.PointerCoords pointerCoords = new MotionEvent.PointerCoords();
        pointerCoords.x = 500f;
        pointerCoords.y = y;
        return MotionEvent.obtain(now, now, action, 1,
            new MotionEvent.PointerProperties[]{pointerProperties},
            new MotionEvent.PointerCoords[]{pointerCoords},
            0, 0, 1f, 1f, 0, 0, 0, 0);
    }

    private MotionEvent event(int action, int pointerCount) {
        long now = 100L;
        MotionEvent.PointerProperties[] properties =
            new MotionEvent.PointerProperties[pointerCount];
        MotionEvent.PointerCoords[] coordinates =
            new MotionEvent.PointerCoords[pointerCount];
        for (int index = 0; index < pointerCount; index++) {
            MotionEvent.PointerProperties pointerProperties = new MotionEvent.PointerProperties();
            pointerProperties.id = index;
            properties[index] = pointerProperties;
            MotionEvent.PointerCoords pointerCoords = new MotionEvent.PointerCoords();
            pointerCoords.x = 10f * (index + 1);
            pointerCoords.y = 20f * (index + 1);
            coordinates[index] = pointerCoords;
        }
        return MotionEvent.obtain(now, now, action, pointerCount, properties, coordinates,
            0, 0, 1f, 1f, 0, 0, 0, 0);
    }

    @Test
    public void doesNotInterceptAPinchGestureSoTheWebViewReceivesIt() {
        BrowserPinchAwareSwipeRefreshLayout layout = newLayout();
        layout.onInterceptTouchEvent(event(MotionEvent.ACTION_DOWN, 1));
        boolean intercepted = layout.onInterceptTouchEvent(
            event(MotionEvent.ACTION_POINTER_DOWN, 2));
        Assert.assertFalse(intercepted);
    }

    @Test
    public void doesNotInterceptTheSingleFingerTailOfAPinchGesture() {
        BrowserPinchAwareSwipeRefreshLayout layout = newLayout();
        layout.onInterceptTouchEvent(event(MotionEvent.ACTION_DOWN, 1));
        layout.onInterceptTouchEvent(event(MotionEvent.ACTION_POINTER_DOWN, 2));
        boolean intercepted = layout.onInterceptTouchEvent(
            event(MotionEvent.ACTION_MOVE, 1));
        Assert.assertFalse(intercepted);
    }

    @Test
    public void doesNotInterceptADownwardDragWhileTheChildCanStillScrollUp() {
        BrowserPinchAwareSwipeRefreshLayout layout = newLayout(true);
        layout.onInterceptTouchEvent(verticalEvent(MotionEvent.ACTION_DOWN, 200f));
        boolean intercepted = layout.onInterceptTouchEvent(
            verticalEvent(MotionEvent.ACTION_MOVE, 600f));
        Assert.assertFalse(intercepted);
    }

    @Test
    public void doesNotInterceptAnUpwardDragWhileTheChildCanStillScrollUp() {
        BrowserPinchAwareSwipeRefreshLayout layout = newLayout(true);
        layout.onInterceptTouchEvent(verticalEvent(MotionEvent.ACTION_DOWN, 600f));
        boolean intercepted = layout.onInterceptTouchEvent(
            verticalEvent(MotionEvent.ACTION_MOVE, 200f));
        Assert.assertFalse(intercepted);
    }

    @Test
    public void doesNotInterceptADownwardDragEvenAfterChildScrollUpTransitionsToFalseWithinTheSameGesture() {
        boolean[] canScrollUp = {true};
        Activity activity = Robolectric.buildActivity(Activity.class).create().get();
        BrowserPinchAwareSwipeRefreshLayout layout = new BrowserPinchAwareSwipeRefreshLayout(activity);
        layout.addView(new View(activity), new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        layout.setOnChildScrollUpCallback((parent, child) -> canScrollUp[0]);
        layout.measure(
            View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY));
        layout.layout(0, 0, 1000, 1000);

        layout.onInterceptTouchEvent(verticalEvent(MotionEvent.ACTION_DOWN, 200f));
        layout.onInterceptTouchEvent(verticalEvent(MotionEvent.ACTION_MOVE, 600f));

        canScrollUp[0] = false;

        boolean intercepted = layout.onInterceptTouchEvent(verticalEvent(MotionEvent.ACTION_MOVE, 800f));
        Assert.assertFalse(intercepted);
    }
}
