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
        Assert.assertTrue(gate.shouldSuppressRefresh(MotionEvent.ACTION_DOWN, true));
    }

    @Test
    public void allowsRefreshAtGestureStartWhenTheChildIsAtTheVeryTop() {
        BrowserScrollUpSuppressionGate gate = new BrowserScrollUpSuppressionGate();
        Assert.assertFalse(gate.shouldSuppressRefresh(MotionEvent.ACTION_DOWN, false));
    }

    @Test
    public void suppressesRefreshOnEveryMoveWhileTheChildCanStillScrollUp() {
        BrowserScrollUpSuppressionGate gate = new BrowserScrollUpSuppressionGate();
        gate.shouldSuppressRefresh(MotionEvent.ACTION_DOWN, true);
        Assert.assertTrue(gate.shouldSuppressRefresh(MotionEvent.ACTION_MOVE, true));
        Assert.assertTrue(gate.shouldSuppressRefresh(MotionEvent.ACTION_MOVE, true));
    }

    @Test
    public void allowsRefreshOnAMoveOnceTheChildReachesTheVeryTop() {
        BrowserScrollUpSuppressionGate gate = new BrowserScrollUpSuppressionGate();
        gate.shouldSuppressRefresh(MotionEvent.ACTION_DOWN, true);
        gate.shouldSuppressRefresh(MotionEvent.ACTION_MOVE, true);
        Assert.assertFalse(gate.shouldSuppressRefresh(MotionEvent.ACTION_MOVE, false));
    }

    @Test
    public void doesNotSuppressMoveEventsThatArriveWithoutAPrecedingDown() {
        BrowserScrollUpSuppressionGate gate = new BrowserScrollUpSuppressionGate();
        Assert.assertFalse(gate.shouldSuppressRefresh(MotionEvent.ACTION_MOVE, true));
    }

    @Test
    public void allowsADeliberateDownwardPullToArmRefreshAfterTheGestureResets() {
        BrowserScrollUpSuppressionGate gate = new BrowserScrollUpSuppressionGate();
        gate.shouldSuppressRefresh(MotionEvent.ACTION_DOWN, true);
        gate.shouldSuppressRefresh(MotionEvent.ACTION_MOVE, true);
        gate.shouldSuppressRefresh(MotionEvent.ACTION_UP, false);
        gate.shouldSuppressRefresh(MotionEvent.ACTION_DOWN, false);
        Assert.assertFalse(gate.shouldSuppressRefresh(MotionEvent.ACTION_MOVE, false));
    }

    @Test
    public void resetsOnCancelSoAnInterruptedScrollDoesNotLeakState() {
        BrowserScrollUpSuppressionGate gate = new BrowserScrollUpSuppressionGate();
        gate.shouldSuppressRefresh(MotionEvent.ACTION_DOWN, true);
        gate.shouldSuppressRefresh(MotionEvent.ACTION_MOVE, true);
        gate.shouldSuppressRefresh(MotionEvent.ACTION_CANCEL, true);
        gate.shouldSuppressRefresh(MotionEvent.ACTION_DOWN, false);
        Assert.assertFalse(gate.shouldSuppressRefresh(MotionEvent.ACTION_MOVE, false));
    }
}
