package com.termux.app.browser;

import android.view.MotionEvent;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class BrowserScrollUpSuppressionGateTest {

    @Test
    public void suppressesRefreshAtGestureStartWhenTheChildCanStillScrollUp() {
        BrowserScrollUpSuppressionGate gate = new BrowserScrollUpSuppressionGate();
        Assert.assertTrue(gate.shouldSuppressRefresh(MotionEvent.ACTION_DOWN, true, false));
    }

    @Test
    public void allowsRefreshAtGestureStartWhenTheChildIsAtTheVeryTop() {
        BrowserScrollUpSuppressionGate gate = new BrowserScrollUpSuppressionGate();
        Assert.assertFalse(gate.shouldSuppressRefresh(MotionEvent.ACTION_DOWN, false, false));
    }

    @Test
    public void suppressesRefreshOnEveryMoveWhileTheChildCanStillScrollUp() {
        BrowserScrollUpSuppressionGate gate = new BrowserScrollUpSuppressionGate();
        gate.shouldSuppressRefresh(MotionEvent.ACTION_DOWN, true, false);
        Assert.assertTrue(gate.shouldSuppressRefresh(MotionEvent.ACTION_MOVE, true, false));
        Assert.assertTrue(gate.shouldSuppressRefresh(MotionEvent.ACTION_MOVE, true, false));
    }

    @Test
    public void continuesSuppressionForEntireGestureEvenAfterChildReachesTheVeryTop() {
        BrowserScrollUpSuppressionGate gate = new BrowserScrollUpSuppressionGate();
        gate.shouldSuppressRefresh(MotionEvent.ACTION_DOWN, true, false);
        gate.shouldSuppressRefresh(MotionEvent.ACTION_MOVE, true, false);
        Assert.assertTrue(gate.shouldSuppressRefresh(MotionEvent.ACTION_MOVE, false, false));
    }

    @Test
    public void doesNotSuppressMoveEventsThatArriveWithoutAPrecedingDown() {
        BrowserScrollUpSuppressionGate gate = new BrowserScrollUpSuppressionGate();
        Assert.assertFalse(gate.shouldSuppressRefresh(MotionEvent.ACTION_MOVE, true, false));
    }

    @Test
    public void longPressUnlockOverridesGestureStartSuppressionToAllowRefresh() {
        BrowserScrollUpSuppressionGate gate = new BrowserScrollUpSuppressionGate();
        gate.shouldSuppressRefresh(MotionEvent.ACTION_DOWN, true, false);
        gate.shouldSuppressRefresh(MotionEvent.ACTION_MOVE, true, false);
        Assert.assertFalse(gate.shouldSuppressRefresh(MotionEvent.ACTION_MOVE, false, true));
    }

    @Test
    public void longPressUnlockDoesNotLeakIntoTheNextGesture() {
        BrowserScrollUpSuppressionGate gate = new BrowserScrollUpSuppressionGate();
        gate.shouldSuppressRefresh(MotionEvent.ACTION_DOWN, true, false);
        gate.shouldSuppressRefresh(MotionEvent.ACTION_MOVE, true, false);
        gate.shouldSuppressRefresh(MotionEvent.ACTION_MOVE, false, true);
        gate.shouldSuppressRefresh(MotionEvent.ACTION_UP, false, false);
        gate.shouldSuppressRefresh(MotionEvent.ACTION_DOWN, true, false);
        Assert.assertTrue(gate.shouldSuppressRefresh(MotionEvent.ACTION_MOVE, true, false));
    }

    @Test
    public void allowsADeliberateDownwardPullToArmRefreshAfterTheGestureResets() {
        BrowserScrollUpSuppressionGate gate = new BrowserScrollUpSuppressionGate();
        gate.shouldSuppressRefresh(MotionEvent.ACTION_DOWN, true, false);
        gate.shouldSuppressRefresh(MotionEvent.ACTION_MOVE, true, false);
        gate.shouldSuppressRefresh(MotionEvent.ACTION_UP, false, false);
        gate.shouldSuppressRefresh(MotionEvent.ACTION_DOWN, false, false);
        Assert.assertFalse(gate.shouldSuppressRefresh(MotionEvent.ACTION_MOVE, false, false));
    }

    @Test
    public void resetsOnCancelSoAnInterruptedScrollDoesNotLeakState() {
        BrowserScrollUpSuppressionGate gate = new BrowserScrollUpSuppressionGate();
        gate.shouldSuppressRefresh(MotionEvent.ACTION_DOWN, true, false);
        gate.shouldSuppressRefresh(MotionEvent.ACTION_MOVE, true, false);
        gate.shouldSuppressRefresh(MotionEvent.ACTION_CANCEL, true, false);
        gate.shouldSuppressRefresh(MotionEvent.ACTION_DOWN, false, false);
        Assert.assertFalse(gate.shouldSuppressRefresh(MotionEvent.ACTION_MOVE, false, false));
    }

    @Test
    public void latchesOntoSuppressionWhenChildBecomesScrollableAfterGestureStartedAtTop() {
        BrowserScrollUpSuppressionGate gate = new BrowserScrollUpSuppressionGate();
        gate.shouldSuppressRefresh(MotionEvent.ACTION_DOWN, false, false);
        Assert.assertTrue(gate.shouldSuppressRefresh(MotionEvent.ACTION_MOVE, true, false));
        Assert.assertTrue(gate.shouldSuppressRefresh(MotionEvent.ACTION_MOVE, true, false));
    }
}
