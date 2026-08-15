package com.termux.app.ownercall;

import org.junit.Assert;
import org.junit.Test;

public class OwnerCallDialogDragGestureTest {

    private static final int TOUCH_SLOP_PIXELS = 24;

    @Test
    public void reportsNoMovementWhileTheTouchStaysInsideTheTouchSlop() {
        OwnerCallDialogDragGesture gesture = new OwnerCallDialogDragGesture(TOUCH_SLOP_PIXELS);

        gesture.onTouchDown(100f, 200f);

        Assert.assertNull("a tap that never leaves the touch slop must not move the dialog",
            gesture.onTouchMoved(110f, 210f));
        Assert.assertFalse(gesture.isDragging());
        Assert.assertFalse("a tap must not be reported as a finished drag",
            gesture.onTouchFinished());
    }

    @Test
    public void reportsTheMovementOnceTheTouchPassesTheTouchSlop() {
        OwnerCallDialogDragGesture gesture = new OwnerCallDialogDragGesture(TOUCH_SLOP_PIXELS);
        gesture.onTouchDown(100f, 200f);

        OwnerCallDialogDragStep firstStep = gesture.onTouchMoved(140f, 200f);
        OwnerCallDialogDragStep secondStep = gesture.onTouchMoved(150f, 230f);

        Assert.assertNotNull(firstStep);
        Assert.assertEquals(0, firstStep.getHorizontalPixels());
        Assert.assertEquals(0, firstStep.getVerticalPixels());
        Assert.assertNotNull(secondStep);
        Assert.assertEquals(10, secondStep.getHorizontalPixels());
        Assert.assertEquals(30, secondStep.getVerticalPixels());
        Assert.assertTrue(gesture.isDragging());
        Assert.assertTrue("a drag that moved must be reported as a finished drag",
            gesture.onTouchFinished());
    }

    @Test
    public void reportsNothingWhileNoTouchIsBeingTracked() {
        OwnerCallDialogDragGesture gesture = new OwnerCallDialogDragGesture(TOUCH_SLOP_PIXELS);

        Assert.assertNull(gesture.onTouchMoved(140f, 200f));
    }

    @Test
    public void startsAFreshGestureAfterTheTouchFinished() {
        OwnerCallDialogDragGesture gesture = new OwnerCallDialogDragGesture(TOUCH_SLOP_PIXELS);
        gesture.onTouchDown(100f, 200f);
        gesture.onTouchMoved(140f, 200f);
        gesture.onTouchFinished();

        gesture.onTouchDown(300f, 400f);

        Assert.assertNull("the second gesture must start from its own touch slop again",
            gesture.onTouchMoved(310f, 410f));
    }
}
