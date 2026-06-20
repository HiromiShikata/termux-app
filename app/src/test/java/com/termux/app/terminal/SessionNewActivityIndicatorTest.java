package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class SessionNewActivityIndicatorTest {

    @Test
    public void nullBellProducesNoIndicatorAndEmptyLabel() {
        SessionNewActivityIndicator indicator = SessionNewActivityIndicator.labelFor(null, 1_000L, 31_000L);

        Assert.assertFalse(indicator.isVisible());
        Assert.assertEquals("", indicator.getLabel());
    }

    @Test
    public void bellBeforeLastSeenProducesNoIndicator() {
        SessionNewActivityIndicator indicator = SessionNewActivityIndicator.labelFor(1_000L, 5_000L, 31_000L);

        Assert.assertFalse(indicator.isVisible());
        Assert.assertEquals("", indicator.getLabel());
    }

    @Test
    public void bellEqualToLastSeenProducesNoIndicator() {
        SessionNewActivityIndicator indicator = SessionNewActivityIndicator.labelFor(5_000L, 5_000L, 31_000L);

        Assert.assertFalse(indicator.isVisible());
        Assert.assertEquals("", indicator.getLabel());
    }

    @Test
    public void bellAfterLastSeenProducesVisibleIndicatorWithRelativeTimeLabel() {
        SessionNewActivityIndicator indicator = SessionNewActivityIndicator.labelFor(1_000L, 500L, 31_000L);

        Assert.assertTrue(indicator.isVisible());
        Assert.assertEquals("30s ago", indicator.getLabel());
    }

    @Test
    public void bellWithoutAnyLastSeenProducesVisibleIndicator() {
        SessionNewActivityIndicator indicator = SessionNewActivityIndicator.labelFor(1_000L, null, 31_000L);

        Assert.assertTrue(indicator.isVisible());
        Assert.assertEquals("30s ago", indicator.getLabel());
    }

    @Test
    public void labelAdvancesAsTimePassesForAFixedBellTime() {
        SessionNewActivityIndicator atFirstTick =
            SessionNewActivityIndicator.labelFor(1_000L, null, 1_000L + 5_000L);
        SessionNewActivityIndicator atLaterTick =
            SessionNewActivityIndicator.labelFor(1_000L, null, 1_000L + 40_000L);

        Assert.assertEquals("5s ago", atFirstTick.getLabel());
        Assert.assertEquals("40s ago", atLaterTick.getLabel());
    }

    @Test
    public void negativeElapsedClampsToZeroSecondsAgo() {
        SessionNewActivityIndicator indicator = SessionNewActivityIndicator.labelFor(5_000L, null, 1_000L);

        Assert.assertTrue(indicator.isVisible());
        Assert.assertEquals("0s ago", indicator.getLabel());
    }
}
