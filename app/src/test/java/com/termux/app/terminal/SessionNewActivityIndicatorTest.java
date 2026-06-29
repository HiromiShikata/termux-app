package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class SessionNewActivityIndicatorTest {

    @Test
    public void noSignalsProduceNoIndicatorAndEmptyLabel() {
        SessionNewActivityIndicator indicator =
            SessionNewActivityIndicator.indicatorFor(null, null, null, null, null, 1_000L, 31_000L);

        Assert.assertFalse(indicator.isVisible());
        Assert.assertEquals(SessionNewActivityTier.NONE, indicator.getTier());
        Assert.assertEquals("", indicator.getLabel());
    }

    @Test
    public void recentOutputBeforeLastSeenStillProducesYellowBecauseYellowDoesNotClearOnView() {
        SessionNewActivityIndicator indicator =
            SessionNewActivityIndicator.indicatorFor(1_000L, null, null, null, null, 5_000L, 31_000L);

        Assert.assertTrue(indicator.isVisible());
        Assert.assertEquals(SessionNewActivityTier.YELLOW, indicator.getTier());
    }

    @Test
    public void recentOutputEqualToLastSeenStillProducesYellow() {
        SessionNewActivityIndicator indicator =
            SessionNewActivityIndicator.indicatorFor(5_000L, null, null, null, null, 5_000L, 31_000L);

        Assert.assertTrue(indicator.isVisible());
        Assert.assertEquals(SessionNewActivityTier.YELLOW, indicator.getTier());
    }

    @Test
    public void idleOutputOlderThanTenMinutesProducesGrayIndicator() {
        long lastOutputActivity = 1_000L;
        long elevenMinutesLater = lastOutputActivity + 11L * 60L * 1000L;

        SessionNewActivityIndicator indicator =
            SessionNewActivityIndicator.indicatorFor(lastOutputActivity, null, null, null, null, 500L, elevenMinutesLater);

        Assert.assertTrue(indicator.isVisible());
        Assert.assertEquals(SessionNewActivityTier.GRAY, indicator.getTier());
    }

    @Test
    public void outputActivityWithinTenMinutesProducesYellowIndicatorWithRelativeTimeLabel() {
        SessionNewActivityIndicator indicator =
            SessionNewActivityIndicator.indicatorFor(1_000L, null, null, null, null, 500L, 31_000L);

        Assert.assertTrue(indicator.isVisible());
        Assert.assertEquals(SessionNewActivityTier.YELLOW, indicator.getTier());
        Assert.assertEquals("30s ago", indicator.getLabel());
    }

    @Test
    public void explicitCallWithoutUserReplyProducesRedIndicatorWithRelativeTimeLabel() {
        SessionNewActivityIndicator indicator =
            SessionNewActivityIndicator.indicatorFor(null, null, 1_000L, null, null, 500L, 31_000L);

        Assert.assertTrue(indicator.isVisible());
        Assert.assertEquals(SessionNewActivityTier.RED, indicator.getTier());
        Assert.assertEquals("30s ago", indicator.getLabel());
    }

    @Test
    public void explicitCallTakesPriorityOverOutputActivity() {
        SessionNewActivityIndicator indicator =
            SessionNewActivityIndicator.indicatorFor(2_000L, null, 1_000L, null, null, 500L, 31_000L);

        Assert.assertEquals(SessionNewActivityTier.RED, indicator.getTier());
        Assert.assertEquals("30s ago", indicator.getLabel());
    }

    @Test
    public void answeredExplicitCallFallsBackToRecentOutputActivityAsYellow() {
        SessionNewActivityIndicator indicator =
            SessionNewActivityIndicator.indicatorFor(2_000L, null, 1_000L, null, 1_500L, 1_500L, 32_000L);

        Assert.assertEquals(SessionNewActivityTier.YELLOW, indicator.getTier());
        Assert.assertEquals("30s ago", indicator.getLabel());
    }

    @Test
    public void unansweredExplicitCallStaysRedEvenWhenOutputActivityIsMoreRecentThanTheCall() {
        SessionNewActivityIndicator indicator =
            SessionNewActivityIndicator.indicatorFor(9_000L, null, 2_000L, null, null, 1_000L, 32_000L);

        Assert.assertEquals(SessionNewActivityTier.RED, indicator.getTier());
        Assert.assertEquals("30s ago", indicator.getLabel());
    }

    @Test
    public void recentOutputWithoutAnyLastSeenProducesVisibleYellowIndicator() {
        SessionNewActivityIndicator indicator =
            SessionNewActivityIndicator.indicatorFor(1_000L, null, null, null, null, null, 31_000L);

        Assert.assertTrue(indicator.isVisible());
        Assert.assertEquals(SessionNewActivityTier.YELLOW, indicator.getTier());
        Assert.assertEquals("30s ago", indicator.getLabel());
    }

    @Test
    public void labelAdvancesAsTimePassesForAFixedSignalTime() {
        SessionNewActivityIndicator atFirstTick =
            SessionNewActivityIndicator.indicatorFor(null, null, 1_000L, null, null, null, 1_000L + 5_000L);
        SessionNewActivityIndicator atLaterTick =
            SessionNewActivityIndicator.indicatorFor(null, null, 1_000L, null, null, null, 1_000L + 40_000L);

        Assert.assertEquals("5s ago", atFirstTick.getLabel());
        Assert.assertEquals("40s ago", atLaterTick.getLabel());
    }

    @Test
    public void yellowLabelAgeIncrementsForAFixedOutputActivityTimeRatherThanStayingAtZero() {
        SessionNewActivityIndicator atFirstTick =
            SessionNewActivityIndicator.indicatorFor(1_000L, null, null, null, null, 500L, 30_500L);
        SessionNewActivityIndicator atLaterTick =
            SessionNewActivityIndicator.indicatorFor(1_000L, null, null, null, null, 500L, 30_500L + 15_000L);

        Assert.assertEquals(SessionNewActivityTier.YELLOW, atFirstTick.getTier());
        Assert.assertEquals("29s ago", atFirstTick.getLabel());
        Assert.assertEquals(SessionNewActivityTier.YELLOW, atLaterTick.getTier());
        Assert.assertEquals("44s ago", atLaterTick.getLabel());
    }

    @Test
    public void negativeElapsedClampsToZeroSecondsAgo() {
        SessionNewActivityIndicator indicator =
            SessionNewActivityIndicator.indicatorFor(null, null, 5_000L, null, null, null, 1_000L);

        Assert.assertTrue(indicator.isVisible());
        Assert.assertEquals("0s ago", indicator.getLabel());
    }
}
