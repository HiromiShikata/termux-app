package com.termux.app.browser;

import android.view.MotionEvent;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class BrowserScrollDirectionInterceptGateTest {

    @Test
    public void allowsInterceptionWhenTheGestureStartsWithTheFingerDown() {
        BrowserScrollDirectionInterceptGate gate = new BrowserScrollDirectionInterceptGate();
        Assert.assertFalse(gate.shouldDeclineInterception(MotionEvent.ACTION_DOWN, 500f));
    }

    @Test
    public void declinesInterceptionWhenTheFingerMovesUpwardBeyondTheSlop() {
        BrowserScrollDirectionInterceptGate gate = new BrowserScrollDirectionInterceptGate();
        gate.shouldDeclineInterception(MotionEvent.ACTION_DOWN, 500f);
        Assert.assertTrue(gate.shouldDeclineInterception(MotionEvent.ACTION_MOVE, 400f));
    }

    @Test
    public void allowsInterceptionForADeliberateDownwardPull() {
        BrowserScrollDirectionInterceptGate gate = new BrowserScrollDirectionInterceptGate();
        gate.shouldDeclineInterception(MotionEvent.ACTION_DOWN, 100f);
        Assert.assertFalse(gate.shouldDeclineInterception(MotionEvent.ACTION_MOVE, 300f));
    }

    @Test
    public void keepsInterceptionAllowedForTinyMovementsWithinTheSlop() {
        BrowserScrollDirectionInterceptGate gate = new BrowserScrollDirectionInterceptGate();
        gate.shouldDeclineInterception(MotionEvent.ACTION_DOWN, 500f);
        Assert.assertFalse(gate.shouldDeclineInterception(
            MotionEvent.ACTION_MOVE, 500f - BrowserScrollDirectionInterceptGate.UPWARD_SCROLL_SLOP_PIXELS));
    }

    @Test
    public void declinesInterceptionOnlyJustPastTheUpwardSlopThreshold() {
        BrowserScrollDirectionInterceptGate gate = new BrowserScrollDirectionInterceptGate();
        gate.shouldDeclineInterception(MotionEvent.ACTION_DOWN, 500f);
        Assert.assertTrue(gate.shouldDeclineInterception(
            MotionEvent.ACTION_MOVE, 500f - BrowserScrollDirectionInterceptGate.UPWARD_SCROLL_SLOP_PIXELS - 1f));
    }

    @Test
    public void continuesDecliningForTheRemainderOfAnUpwardFlingThatReachesTheTop() {
        BrowserScrollDirectionInterceptGate gate = new BrowserScrollDirectionInterceptGate();
        gate.shouldDeclineInterception(MotionEvent.ACTION_DOWN, 900f);
        Assert.assertTrue(gate.shouldDeclineInterception(MotionEvent.ACTION_MOVE, 700f));
        Assert.assertTrue(gate.shouldDeclineInterception(MotionEvent.ACTION_MOVE, 400f));
        Assert.assertTrue(gate.shouldDeclineInterception(MotionEvent.ACTION_MOVE, 100f));
    }

    @Test
    public void resetsAfterTheGestureEndsSoTheNextDownwardPullCanArmRefresh() {
        BrowserScrollDirectionInterceptGate gate = new BrowserScrollDirectionInterceptGate();
        gate.shouldDeclineInterception(MotionEvent.ACTION_DOWN, 500f);
        gate.shouldDeclineInterception(MotionEvent.ACTION_MOVE, 300f);
        gate.shouldDeclineInterception(MotionEvent.ACTION_UP, 300f);
        gate.shouldDeclineInterception(MotionEvent.ACTION_DOWN, 100f);
        Assert.assertFalse(gate.shouldDeclineInterception(MotionEvent.ACTION_MOVE, 300f));
    }

    @Test
    public void resetsOnCancelSoAnInterruptedScrollDoesNotLeakState() {
        BrowserScrollDirectionInterceptGate gate = new BrowserScrollDirectionInterceptGate();
        gate.shouldDeclineInterception(MotionEvent.ACTION_DOWN, 500f);
        gate.shouldDeclineInterception(MotionEvent.ACTION_MOVE, 300f);
        gate.shouldDeclineInterception(MotionEvent.ACTION_CANCEL, 300f);
        gate.shouldDeclineInterception(MotionEvent.ACTION_DOWN, 100f);
        Assert.assertFalse(gate.shouldDeclineInterception(MotionEvent.ACTION_MOVE, 300f));
    }

    @Test
    public void doesNotDeclineMoveEventsThatArriveWithoutAPrecedingDown() {
        BrowserScrollDirectionInterceptGate gate = new BrowserScrollDirectionInterceptGate();
        Assert.assertFalse(gate.shouldDeclineInterception(MotionEvent.ACTION_MOVE, 100f));
    }

    @Test
    public void reportsUpwardScrollPurelyFromTheTwoCoordinates() {
        Assert.assertTrue(BrowserScrollDirectionInterceptGate.isUpwardScroll(500f, 400f));
        Assert.assertFalse(BrowserScrollDirectionInterceptGate.isUpwardScroll(100f, 300f));
        Assert.assertFalse(BrowserScrollDirectionInterceptGate.isUpwardScroll(500f, 500f));
    }
}
