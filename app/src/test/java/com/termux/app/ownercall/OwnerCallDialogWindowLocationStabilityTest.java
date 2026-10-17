package com.termux.app.ownercall;

import org.junit.Assert;
import org.junit.Test;

public class OwnerCallDialogWindowLocationStabilityTest {

    private static boolean settled(int previousWindowRelativeYPixels,
                                    int previousWindowHeightPixels,
                                    int currentWindowRelativeYPixels,
                                    int currentWindowHeightPixels,
                                    int viewHeightPixels) {
        return OwnerCallDialogWindowLocationStability.hasSettled(previousWindowRelativeYPixels,
            previousWindowHeightPixels, currentWindowRelativeYPixels, currentWindowHeightPixels,
            viewHeightPixels);
    }

    @Test
    public void aNegativeCurrentYIsNeverSettledEvenWhenUnchangedFromThePrevious() {
        Assert.assertFalse(settled(-527, 1920, -527, 1920, 100));
    }

    @Test
    public void aViewFlushWithTheVeryBottomEdgeOfTheWindowCountsAsWithinBoundsAndSettled() {
        Assert.assertTrue(settled(900, 1000, 900, 1000, 100));
    }

    @Test
    public void aViewExtendingBelowTheWindowIsNeverSettledEvenWhenUnchangedFromThePrevious() {
        Assert.assertFalse(settled(950, 1000, 950, 1000, 100));
    }

    @Test
    public void aCurrentYThatDiffersFromThePreviousYIsNotYetSettled() {
        Assert.assertFalse(settled(90, 1000, 100, 1000, 100));
    }

    @Test
    public void aCurrentWindowHeightThatDiffersFromThePreviousWindowHeightIsNotYetSettled() {
        Assert.assertFalse(settled(100, 900, 100, 1000, 100));
    }

    @Test
    public void validBoundsWithBothYAndWindowHeightUnchangedFromThePreviousCheckIsSettled() {
        Assert.assertTrue(settled(100, 1000, 100, 1000, 100));
    }

    @Test
    public void aViewAtTheVeryTopOfTheWindowWithNoMovementIsSettled() {
        Assert.assertTrue(settled(0, 1000, 0, 1000, 100));
    }

    @Test
    public void aNegativeCurrentYThatAlsoChangedFromThePreviousIsNotSettled() {
        Assert.assertFalse(settled(-523, 1857, -481, 1920, 100));
    }
}
