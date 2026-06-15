package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class SessionListBottomSheetDragDecisionTest {

    @Test
    public void defaultHeightIsOneThirdOfScreen() {
        Assert.assertEquals(640, SessionListBottomSheetDragDecision.computeDefaultHeight(1920));
    }

    @Test
    public void minHeightIsOneQuarterOfScreen() {
        Assert.assertEquals(480, SessionListBottomSheetDragDecision.computeMinHeight(1920));
    }

    @Test
    public void maxHeightIsEightyFivePercentOfScreen() {
        Assert.assertEquals(1632, SessionListBottomSheetDragDecision.computeMaxHeight(1920));
    }

    @Test
    public void minIsBelowDefaultAndDefaultIsBelowMaxSoTheSheetCanGrowAndShrink() {
        int screenHeightPixels = 1920;
        int minHeightPixels = SessionListBottomSheetDragDecision.computeMinHeight(screenHeightPixels);
        int defaultHeightPixels = SessionListBottomSheetDragDecision.computeDefaultHeight(screenHeightPixels);
        int maxHeightPixels = SessionListBottomSheetDragDecision.computeMaxHeight(screenHeightPixels);
        Assert.assertTrue(minHeightPixels < defaultHeightPixels);
        Assert.assertTrue(defaultHeightPixels < maxHeightPixels);
    }

    @Test
    public void draggingUpGrowsTheSheetTaller() {
        Assert.assertEquals(800, SessionListBottomSheetDragDecision.resolveDragHeight(-200f, 600, 480, 1632));
    }

    @Test
    public void draggingDownShrinksTheSheetShorter() {
        Assert.assertEquals(500, SessionListBottomSheetDragDecision.resolveDragHeight(100f, 600, 480, 1632));
    }

    @Test
    public void draggingUpStopsGrowingAtMaxHeight() {
        Assert.assertEquals(1632, SessionListBottomSheetDragDecision.resolveDragHeight(-5000f, 600, 480, 1632));
    }

    @Test
    public void draggingDownStopsShrinkingAtMinHeight() {
        Assert.assertEquals(480, SessionListBottomSheetDragDecision.resolveDragHeight(5000f, 600, 480, 1632));
    }

    @Test
    public void translationStaysZeroWhileTheSheetIsStillAtOrAboveMinHeight() {
        Assert.assertEquals(0f, SessionListBottomSheetDragDecision.resolveDragTranslation(100f, 600, 480), 0f);
    }

    @Test
    public void translationFollowsTheFingerOnceTheSheetIsPulledBelowMinHeight() {
        Assert.assertEquals(80f, SessionListBottomSheetDragDecision.resolveDragTranslation(200f, 600, 480), 0f);
    }

    @Test
    public void dismissesWhenPulledWellBelowMinHeight() {
        Assert.assertTrue(SessionListBottomSheetDragDecision.shouldDismissAfterDrag(320f, 600, 480, 0f));
    }

    @Test
    public void springsBackWhenPulledOnlySlightlyBelowMinHeightWithLowVelocity() {
        Assert.assertFalse(SessionListBottomSheetDragDecision.shouldDismissAfterDrag(200f, 600, 480, 0f));
    }

    @Test
    public void doesNotDismissWhenStillAboveMinHeight() {
        Assert.assertFalse(SessionListBottomSheetDragDecision.shouldDismissAfterDrag(50f, 600, 480, 0f));
    }

    @Test
    public void dismissesOnFastDownwardFlingEvenWhenStillAboveMinHeight() {
        Assert.assertTrue(SessionListBottomSheetDragDecision.shouldDismissAfterDrag(20f, 600, 480, 1200f));
    }

    @Test
    public void doesNotDismissOnUpwardVelocityWhilePulledBelowMin() {
        Assert.assertFalse(SessionListBottomSheetDragDecision.shouldDismissAfterDrag(140f, 600, 480, -1200f));
    }
}
