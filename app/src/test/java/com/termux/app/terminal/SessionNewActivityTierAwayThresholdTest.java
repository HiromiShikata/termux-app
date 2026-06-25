package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class SessionNewActivityTierAwayThresholdTest {

    private static final long SEEN = 100_000L;

    @Test
    public void yellowIsSuppressedWhileTheUserWasAwayForLessThanTheThreshold() {
        long lastOutputActivity = SEEN + 1L;
        long nowJustAfterLeaving = SEEN + 5_000L;

        SessionNewActivityTier tier = SessionNewActivityTier.resolve(
            lastOutputActivity, null, null, SEEN, nowJustAfterLeaving);

        Assert.assertEquals(SessionNewActivityTier.NONE, tier);
    }

    @Test
    public void yellowAppearsOnceTheUserHasBeenAwayLongerThanTheThreshold() {
        long lastOutputActivity = SEEN + 1L;
        long nowAfterThreshold = SEEN + SessionNewActivityTier.YELLOW_MIN_AWAY_MILLIS;

        SessionNewActivityTier tier = SessionNewActivityTier.resolve(
            lastOutputActivity, null, null, SEEN, nowAfterThreshold);

        Assert.assertEquals(SessionNewActivityTier.YELLOW, tier);
    }

    @Test
    public void yellowClearsImmediatelyWhenTheSessionIsViewedAgainEvenWithContinuousOutput() {
        long lastOutputActivity = SEEN + SessionNewActivityTier.YELLOW_MIN_AWAY_MILLIS + 10_000L;
        long lastSeenAfterViewing = lastOutputActivity + 1L;
        long now = lastOutputActivity + 2L;

        SessionNewActivityTier tier = SessionNewActivityTier.resolve(
            lastOutputActivity, null, null, lastSeenAfterViewing, now);

        Assert.assertEquals(SessionNewActivityTier.NONE, tier);
    }

    @Test
    public void neverSeenSessionWithOutputShowsYellowRegardlessOfThreshold() {
        long lastOutputActivity = 5_000L;
        long now = 5_100L;

        SessionNewActivityTier tier = SessionNewActivityTier.resolve(
            lastOutputActivity, null, null, null, now);

        Assert.assertEquals(SessionNewActivityTier.YELLOW, tier);
    }

    @Test
    public void yellowAppearsWhenRealOutputIsOneMinuteAgoAndUserIsAway() {
        long lastOutputActivity = SEEN + 1L;
        long oneMinute = 60L * 1000L;
        long nowOneMinuteAfterOutput = lastOutputActivity + oneMinute;

        SessionNewActivityTier tier = SessionNewActivityTier.resolve(
            lastOutputActivity, null, null, SEEN, nowOneMinuteAfterOutput);

        Assert.assertEquals(SessionNewActivityTier.YELLOW, tier);
    }

    @Test
    public void noneWhenRealOutputIsElevenMinutesAgo() {
        long lastOutputActivity = SEEN + 1L;
        long elevenMinutes = 11L * 60L * 1000L;
        long nowElevenMinutesAfterOutput = lastOutputActivity + elevenMinutes;

        SessionNewActivityTier tier = SessionNewActivityTier.resolve(
            lastOutputActivity, null, null, SEEN, nowElevenMinutesAfterOutput);

        Assert.assertEquals(SessionNewActivityTier.NONE, tier);
    }

    @Test
    public void exactlyTenMinutesAgoIsStillYellowBecauseTheRecencyBoundaryIsInclusive() {
        long lastOutputActivity = SEEN + 1L;
        long nowAtRecencyBoundary = lastOutputActivity + SessionNewActivityTier.YELLOW_MAX_AGE_MILLIS;

        SessionNewActivityTier tier = SessionNewActivityTier.resolve(
            lastOutputActivity, null, null, SEEN, nowAtRecencyBoundary);

        Assert.assertEquals(SessionNewActivityTier.YELLOW, tier);
    }

    @Test
    public void noneOneMillisecondPastTheTenMinuteRecencyBoundary() {
        long lastOutputActivity = SEEN + 1L;
        long nowJustPastRecencyBoundary = lastOutputActivity + SessionNewActivityTier.YELLOW_MAX_AGE_MILLIS + 1L;

        SessionNewActivityTier tier = SessionNewActivityTier.resolve(
            lastOutputActivity, null, null, SEEN, nowJustPastRecencyBoundary);

        Assert.assertEquals(SessionNewActivityTier.NONE, tier);
    }

    @Test
    public void redExplicitCallIsNeverDelayedByTheAwayThreshold() {
        long lastExplicitCall = SEEN + 1L;
        long nowJustAfterLeaving = SEEN + 5L;

        SessionNewActivityTier tier = SessionNewActivityTier.resolve(
            null, lastExplicitCall, null, SEEN, nowJustAfterLeaving);

        Assert.assertEquals(SessionNewActivityTier.RED, tier);
    }

    @Test
    public void threeArgResolvePreservesLegacyImmediateYellowWithoutThreshold() {
        long lastOutputActivity = SEEN + 1L;

        SessionNewActivityTier tier = SessionNewActivityTier.resolve(
            lastOutputActivity, null, null, SEEN);

        Assert.assertEquals(SessionNewActivityTier.YELLOW, tier);
    }

    @Test
    public void threeArgResolveStillReturnsNoneWhenSignalIsNotNewerThanSeen() {
        SessionNewActivityTier tier = SessionNewActivityTier.resolve(
            SEEN, null, null, SEEN);

        Assert.assertEquals(SessionNewActivityTier.NONE, tier);
    }

    @Test
    public void indicatorAppliesTheAwayThresholdToTheYellowDot() {
        long lastOutputActivity = SEEN + 1L;
        long nowJustAfterLeaving = SEEN + 5_000L;

        SessionNewActivityIndicator indicator = SessionNewActivityIndicator.indicatorFor(
            lastOutputActivity, null, null, SEEN, nowJustAfterLeaving);

        Assert.assertFalse(indicator.isVisible());

        long nowAfterThreshold = SEEN + SessionNewActivityTier.YELLOW_MIN_AWAY_MILLIS + 2_000L;
        SessionNewActivityIndicator indicatorAfterThreshold = SessionNewActivityIndicator.indicatorFor(
            lastOutputActivity, null, null, SEEN, nowAfterThreshold);

        Assert.assertTrue(indicatorAfterThreshold.isVisible());
        Assert.assertEquals(SessionNewActivityTier.YELLOW, indicatorAfterThreshold.getTier());
    }
}
