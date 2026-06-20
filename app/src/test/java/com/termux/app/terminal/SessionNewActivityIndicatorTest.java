package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class SessionNewActivityIndicatorTest {

    @Test
    public void timestampPresentProducesVisibleIndicatorWithRelativeTimeLabel() {
        SessionNewActivityIndicator indicator = SessionNewActivityIndicator.labelFor(1_000L, 31_000L);

        Assert.assertTrue(indicator.isVisible());
        Assert.assertEquals("30s ago", indicator.getLabel());
    }

    @Test
    public void timestampPresentProducesMinutesLabelForSubHourElapsed() {
        SessionNewActivityIndicator indicator = SessionNewActivityIndicator.labelFor(0L, 150_000L);

        Assert.assertTrue(indicator.isVisible());
        Assert.assertEquals("2m ago", indicator.getLabel());
    }

    @Test
    public void nullTimestampProducesNoIndicatorAndEmptyLabel() {
        SessionNewActivityIndicator indicator = SessionNewActivityIndicator.labelFor(null, 31_000L);

        Assert.assertFalse(indicator.isVisible());
        Assert.assertEquals("", indicator.getLabel());
    }

    @Test
    public void negativeElapsedClampsToZeroSecondsAgo() {
        SessionNewActivityIndicator indicator = SessionNewActivityIndicator.labelFor(5_000L, 1_000L);

        Assert.assertTrue(indicator.isVisible());
        Assert.assertEquals("0s ago", indicator.getLabel());
    }
}
