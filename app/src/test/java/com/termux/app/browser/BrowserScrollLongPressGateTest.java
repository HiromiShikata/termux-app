package com.termux.app.browser;

import android.view.MotionEvent;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class BrowserScrollLongPressGateTest {

    private static final long THRESHOLD = BrowserScrollLongPressGate.LONG_PRESS_THRESHOLD_MS;
    private static final float SLOP = BrowserScrollLongPressGate.MOVEMENT_CANCEL_SLOP_PIXELS;

    @Test
    public void doesNotUnlockOnActionDown() {
        BrowserScrollLongPressGate gate = new BrowserScrollLongPressGate();
        Assert.assertFalse(gate.isLongPressUnlocked(MotionEvent.ACTION_DOWN, 500f, 0L));
    }

    @Test
    public void doesNotUnlockOnMoveBelowThresholdTimeWithoutMovement() {
        BrowserScrollLongPressGate gate = new BrowserScrollLongPressGate();
        gate.isLongPressUnlocked(MotionEvent.ACTION_DOWN, 500f, 0L);
        Assert.assertFalse(gate.isLongPressUnlocked(MotionEvent.ACTION_MOVE, 500f, THRESHOLD - 1L));
    }

    @Test
    public void unlocksOnMoveAfterThresholdTimeWithoutSignificantMovement() {
        BrowserScrollLongPressGate gate = new BrowserScrollLongPressGate();
        gate.isLongPressUnlocked(MotionEvent.ACTION_DOWN, 500f, 0L);
        Assert.assertTrue(gate.isLongPressUnlocked(MotionEvent.ACTION_MOVE, 500f, THRESHOLD));
    }

    @Test
    public void doesNotUnlockWhenFingerMovesMoreThanSlopBeforeThreshold() {
        BrowserScrollLongPressGate gate = new BrowserScrollLongPressGate();
        gate.isLongPressUnlocked(MotionEvent.ACTION_DOWN, 500f, 0L);
        gate.isLongPressUnlocked(MotionEvent.ACTION_MOVE, 500f + SLOP + 1f, THRESHOLD - 1L);
        Assert.assertFalse(gate.isLongPressUnlocked(MotionEvent.ACTION_MOVE, 500f, THRESHOLD));
    }

    @Test
    public void doesNotUnlockWithinSlopMovementRegardlessOfTime() {
        BrowserScrollLongPressGate gate = new BrowserScrollLongPressGate();
        gate.isLongPressUnlocked(MotionEvent.ACTION_DOWN, 500f, 0L);
        Assert.assertFalse(gate.isLongPressUnlocked(MotionEvent.ACTION_MOVE, 500f + SLOP, THRESHOLD - 1L));
    }

    @Test
    public void unlocksAtExactSlopBoundaryWithSufficientTime() {
        BrowserScrollLongPressGate gate = new BrowserScrollLongPressGate();
        gate.isLongPressUnlocked(MotionEvent.ACTION_DOWN, 500f, 0L);
        Assert.assertTrue(gate.isLongPressUnlocked(MotionEvent.ACTION_MOVE, 500f + SLOP, THRESHOLD));
    }

    @Test
    public void remainsUnlockedAfterThresholdEvenWhenFingerMovesLater() {
        BrowserScrollLongPressGate gate = new BrowserScrollLongPressGate();
        gate.isLongPressUnlocked(MotionEvent.ACTION_DOWN, 500f, 0L);
        gate.isLongPressUnlocked(MotionEvent.ACTION_MOVE, 500f, THRESHOLD);
        Assert.assertTrue(gate.isLongPressUnlocked(MotionEvent.ACTION_MOVE, 800f, THRESHOLD + 100L));
    }

    @Test
    public void resetsUnlockOnActionUp() {
        BrowserScrollLongPressGate gate = new BrowserScrollLongPressGate();
        gate.isLongPressUnlocked(MotionEvent.ACTION_DOWN, 500f, 0L);
        gate.isLongPressUnlocked(MotionEvent.ACTION_MOVE, 500f, THRESHOLD);
        gate.isLongPressUnlocked(MotionEvent.ACTION_UP, 500f, THRESHOLD + 100L);
        long secondDown = THRESHOLD + 200L;
        gate.isLongPressUnlocked(MotionEvent.ACTION_DOWN, 500f, secondDown);
        Assert.assertFalse(gate.isLongPressUnlocked(MotionEvent.ACTION_MOVE, 500f, secondDown + THRESHOLD - 1L));
    }

    @Test
    public void resetsUnlockOnActionCancel() {
        BrowserScrollLongPressGate gate = new BrowserScrollLongPressGate();
        gate.isLongPressUnlocked(MotionEvent.ACTION_DOWN, 500f, 0L);
        gate.isLongPressUnlocked(MotionEvent.ACTION_MOVE, 500f, THRESHOLD);
        gate.isLongPressUnlocked(MotionEvent.ACTION_CANCEL, 500f, THRESHOLD + 100L);
        long secondDown = THRESHOLD + 200L;
        gate.isLongPressUnlocked(MotionEvent.ACTION_DOWN, 500f, secondDown);
        Assert.assertFalse(gate.isLongPressUnlocked(MotionEvent.ACTION_MOVE, 500f, secondDown + THRESHOLD - 1L));
    }

    @Test
    public void doesNotUnlockIfNoActionDownPrecedes() {
        BrowserScrollLongPressGate gate = new BrowserScrollLongPressGate();
        Assert.assertFalse(gate.isLongPressUnlocked(MotionEvent.ACTION_MOVE, 500f, THRESHOLD + 1000L));
    }

    @Test
    public void tracksDownTimeIndependentlyAcrossConsecutiveGestures() {
        BrowserScrollLongPressGate gate = new BrowserScrollLongPressGate();
        gate.isLongPressUnlocked(MotionEvent.ACTION_DOWN, 500f, 0L);
        gate.isLongPressUnlocked(MotionEvent.ACTION_MOVE, 500f, THRESHOLD);
        gate.isLongPressUnlocked(MotionEvent.ACTION_UP, 500f, THRESHOLD + 100L);
        long secondDown = THRESHOLD + 200L;
        gate.isLongPressUnlocked(MotionEvent.ACTION_DOWN, 500f, secondDown);
        Assert.assertFalse(gate.isLongPressUnlocked(MotionEvent.ACTION_MOVE, 500f, secondDown + THRESHOLD - 1L));
    }
}
