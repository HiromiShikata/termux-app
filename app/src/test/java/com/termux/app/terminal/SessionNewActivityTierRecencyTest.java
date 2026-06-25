package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class SessionNewActivityTierRecencyTest {

    private static final long SEEN = 100_000L;
    private static final long ONE_MINUTE_MILLIS = 60L * 1000L;

    @Test
    public void outputOneMinuteAgoIsYellowEvenWhenJustSeen() {
        long lastOutputActivity = SEEN + 1L;
        long lastSeenAfterOutput = lastOutputActivity + 1L;
        long nowOneMinuteAfterOutput = lastOutputActivity + ONE_MINUTE_MILLIS;

        SessionNewActivityTier tier = SessionNewActivityTier.resolve(
            lastOutputActivity, null, null, lastSeenAfterOutput, nowOneMinuteAfterOutput);

        Assert.assertEquals(SessionNewActivityTier.YELLOW, tier);
    }

    @Test
    public void outputElevenMinutesAgoIsGray() {
        long lastOutputActivity = SEEN + 1L;
        long elevenMinutes = 11L * ONE_MINUTE_MILLIS;
        long nowElevenMinutesAfterOutput = lastOutputActivity + elevenMinutes;

        SessionNewActivityTier tier = SessionNewActivityTier.resolve(
            lastOutputActivity, null, null, SEEN, nowElevenMinutesAfterOutput);

        Assert.assertEquals(SessionNewActivityTier.GRAY, tier);
    }

    @Test
    public void noOutputIsNone() {
        long now = SEEN + 1L;

        SessionNewActivityTier tier = SessionNewActivityTier.resolve(
            null, null, null, SEEN, now);

        Assert.assertEquals(SessionNewActivityTier.NONE, tier);
    }

    @Test
    public void pendingCallToUserIsRed() {
        long lastExplicitCall = SEEN + 1L;
        long now = SEEN + 5L;

        SessionNewActivityTier tier = SessionNewActivityTier.resolve(
            null, lastExplicitCall, null, SEEN, now);

        Assert.assertEquals(SessionNewActivityTier.RED, tier);
    }

    @Test
    public void redTakesPriorityOverRecentOutput() {
        long lastOutputActivity = SEEN + 10L;
        long lastExplicitCall = SEEN + 20L;
        long now = SEEN + 30L;

        SessionNewActivityTier tier = SessionNewActivityTier.resolve(
            lastOutputActivity, lastExplicitCall, null, SEEN, now);

        Assert.assertEquals(SessionNewActivityTier.RED, tier);
    }

    @Test
    public void yellowDoesNotClearWhenTheSessionIsViewedWhileOutputStaysRecent() {
        long lastOutputActivity = SEEN + 1L;
        long lastSeenAfterViewing = lastOutputActivity + 1L;
        long now = lastOutputActivity + 2L;

        SessionNewActivityTier tier = SessionNewActivityTier.resolve(
            lastOutputActivity, null, null, lastSeenAfterViewing, now);

        Assert.assertEquals(SessionNewActivityTier.YELLOW, tier);
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
    public void grayOneMillisecondPastTheTenMinuteRecencyBoundary() {
        long lastOutputActivity = SEEN + 1L;
        long nowJustPastRecencyBoundary = lastOutputActivity + SessionNewActivityTier.YELLOW_MAX_AGE_MILLIS + 1L;

        SessionNewActivityTier tier = SessionNewActivityTier.resolve(
            lastOutputActivity, null, null, SEEN, nowJustPastRecencyBoundary);

        Assert.assertEquals(SessionNewActivityTier.GRAY, tier);
    }

    @Test
    public void neverSeenSessionWithRecentOutputShowsYellow() {
        long lastOutputActivity = SEEN + 1L;
        long now = lastOutputActivity + 100L;

        SessionNewActivityTier tier = SessionNewActivityTier.resolve(
            lastOutputActivity, null, null, null, now);

        Assert.assertEquals(SessionNewActivityTier.YELLOW, tier);
    }

    @Test
    public void redExplicitCallIsNeverAffectedByOutputRecency() {
        long lastExplicitCall = SEEN + 1L;
        long staleOutput = SEEN - (20L * ONE_MINUTE_MILLIS);
        long now = SEEN + 5L;

        SessionNewActivityTier tier = SessionNewActivityTier.resolve(
            staleOutput, lastExplicitCall, null, SEEN, now);

        Assert.assertEquals(SessionNewActivityTier.RED, tier);
    }

    @Test
    public void indicatorShowsYellowForRecentOutputRegardlessOfSeen() {
        long lastOutputActivity = SEEN + 1L;
        long lastSeenAfterOutput = lastOutputActivity + 1L;
        long nowOneMinuteAfterOutput = lastOutputActivity + ONE_MINUTE_MILLIS;

        SessionNewActivityIndicator indicator = SessionNewActivityIndicator.indicatorFor(
            lastOutputActivity, null, null, lastSeenAfterOutput, nowOneMinuteAfterOutput);

        Assert.assertTrue(indicator.isVisible());
        Assert.assertEquals(SessionNewActivityTier.YELLOW, indicator.getTier());
    }

    @Test
    public void indicatorShowsGrayForIdleOutputOlderThanTenMinutes() {
        long lastOutputActivity = SEEN + 1L;
        long nowElevenMinutesAfterOutput = lastOutputActivity + (11L * ONE_MINUTE_MILLIS);

        SessionNewActivityIndicator indicator = SessionNewActivityIndicator.indicatorFor(
            lastOutputActivity, null, null, SEEN, nowElevenMinutesAfterOutput);

        Assert.assertTrue(indicator.isVisible());
        Assert.assertEquals(SessionNewActivityTier.GRAY, indicator.getTier());
    }
}
