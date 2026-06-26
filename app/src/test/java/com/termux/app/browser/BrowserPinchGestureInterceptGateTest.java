package com.termux.app.browser;

import android.view.MotionEvent;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class BrowserPinchGestureInterceptGateTest {

    @Test
    public void allowsInterceptionForASingleFingerGesture() {
        BrowserPinchGestureInterceptGate gate = new BrowserPinchGestureInterceptGate();
        Assert.assertFalse(gate.shouldDeclineInterception(MotionEvent.ACTION_DOWN, 1));
        Assert.assertFalse(gate.shouldDeclineInterception(MotionEvent.ACTION_MOVE, 1));
        Assert.assertFalse(gate.shouldDeclineInterception(MotionEvent.ACTION_UP, 1));
    }

    @Test
    public void declinesInterceptionWhenASecondFingerStartsAPinch() {
        BrowserPinchGestureInterceptGate gate = new BrowserPinchGestureInterceptGate();
        gate.shouldDeclineInterception(MotionEvent.ACTION_DOWN, 1);
        Assert.assertTrue(gate.shouldDeclineInterception(MotionEvent.ACTION_POINTER_DOWN, 2));
        Assert.assertTrue(gate.isMultiTouchGestureInProgress());
    }

    @Test
    public void keepsDecliningForTheRemainderOfAGestureThatBecameAPinch() {
        BrowserPinchGestureInterceptGate gate = new BrowserPinchGestureInterceptGate();
        gate.shouldDeclineInterception(MotionEvent.ACTION_DOWN, 1);
        gate.shouldDeclineInterception(MotionEvent.ACTION_POINTER_DOWN, 2);
        gate.shouldDeclineInterception(MotionEvent.ACTION_MOVE, 2);
        Assert.assertTrue(gate.shouldDeclineInterception(MotionEvent.ACTION_MOVE, 1));
    }

    @Test
    public void resetsAfterThePinchGestureFinishesSoTapsAndScrollsWorkAgain() {
        BrowserPinchGestureInterceptGate gate = new BrowserPinchGestureInterceptGate();
        gate.shouldDeclineInterception(MotionEvent.ACTION_DOWN, 1);
        gate.shouldDeclineInterception(MotionEvent.ACTION_POINTER_DOWN, 2);
        gate.shouldDeclineInterception(MotionEvent.ACTION_UP, 1);
        Assert.assertFalse(gate.isMultiTouchGestureInProgress());
        Assert.assertFalse(gate.shouldDeclineInterception(MotionEvent.ACTION_DOWN, 1));
        Assert.assertFalse(gate.shouldDeclineInterception(MotionEvent.ACTION_MOVE, 1));
    }

    @Test
    public void resetsOnCancelSoAnInterruptedPinchDoesNotLeakState() {
        BrowserPinchGestureInterceptGate gate = new BrowserPinchGestureInterceptGate();
        gate.shouldDeclineInterception(MotionEvent.ACTION_DOWN, 1);
        gate.shouldDeclineInterception(MotionEvent.ACTION_POINTER_DOWN, 2);
        gate.shouldDeclineInterception(MotionEvent.ACTION_CANCEL, 2);
        Assert.assertFalse(gate.isMultiTouchGestureInProgress());
        Assert.assertFalse(gate.shouldDeclineInterception(MotionEvent.ACTION_DOWN, 1));
    }

    @Test
    public void declinesWhenAGestureBeginsAlreadyWithMultiplePointers() {
        BrowserPinchGestureInterceptGate gate = new BrowserPinchGestureInterceptGate();
        Assert.assertTrue(gate.shouldDeclineInterception(MotionEvent.ACTION_DOWN, 2));
    }
}
