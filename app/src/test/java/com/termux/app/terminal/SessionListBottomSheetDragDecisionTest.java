package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class SessionListBottomSheetDragDecisionTest {

    @Test
    public void clampsUpwardDragToZeroSoSheetCannotMoveAboveOpenPosition() {
        Assert.assertEquals(0f, SessionListBottomSheetDragDecision.clampDragTranslation(-150f, 600f), 0f);
    }

    @Test
    public void clampsDownwardDragToSheetHeightSoSheetCannotMoveBelowFullyHidden() {
        Assert.assertEquals(600f, SessionListBottomSheetDragDecision.clampDragTranslation(900f, 600f), 0f);
    }

    @Test
    public void passesThroughDragTranslationWithinBounds() {
        Assert.assertEquals(250f, SessionListBottomSheetDragDecision.clampDragTranslation(250f, 600f), 0f);
    }

    @Test
    public void dismissesWhenDraggedPastOneThirdOfSheetHeight() {
        Assert.assertTrue(SessionListBottomSheetDragDecision.shouldDismissAfterDrag(250f, 0f, 600f));
    }

    @Test
    public void springsBackWhenDraggedBelowOneThirdWithLowVelocity() {
        Assert.assertFalse(SessionListBottomSheetDragDecision.shouldDismissAfterDrag(100f, 0f, 600f));
    }

    @Test
    public void dismissesOnFastDownwardFlingEvenWhenDragIsShort() {
        Assert.assertTrue(SessionListBottomSheetDragDecision.shouldDismissAfterDrag(40f, 1200f, 600f));
    }

    @Test
    public void springsBackOnUpwardVelocityWhenDragIsShort() {
        Assert.assertFalse(SessionListBottomSheetDragDecision.shouldDismissAfterDrag(40f, -1200f, 600f));
    }
}
