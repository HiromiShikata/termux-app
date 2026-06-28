package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class SessionNewActivityStoreRelativeAgeTest {

    private static final long ONE_SECOND_MILLIS = 1000L;
    private static final long ONE_MINUTE_MILLIS = 60L * ONE_SECOND_MILLIS;
    private static final long ONE_HOUR_MILLIS = 60L * ONE_MINUTE_MILLIS;
    private static final long ONE_DAY_MILLIS = 24L * ONE_HOUR_MILLIS;
    private static final long NOW = 1_000_000_000L;

    @Test
    public void rendersSecondsWithoutAgoSuffix() {
        Assert.assertEquals("45s",
            SessionNewActivityStore.formatRelativeAge(NOW - 45L * ONE_SECOND_MILLIS, NOW));
    }

    @Test
    public void rendersMinutesWithoutAgoSuffix() {
        Assert.assertEquals("12m",
            SessionNewActivityStore.formatRelativeAge(NOW - 12L * ONE_MINUTE_MILLIS, NOW));
    }

    @Test
    public void rendersHoursWithoutAgoSuffix() {
        Assert.assertEquals("3h",
            SessionNewActivityStore.formatRelativeAge(NOW - 3L * ONE_HOUR_MILLIS, NOW));
    }

    @Test
    public void rendersZeroSecondsForTheCurrentInstant() {
        Assert.assertEquals("0s", SessionNewActivityStore.formatRelativeAge(NOW, NOW));
    }

    @Test
    public void rendersMoreThanOneDayWhenTheTimeIsInTheFuture() {
        Assert.assertEquals(">1d",
            SessionNewActivityStore.formatRelativeAge(NOW + ONE_SECOND_MILLIS, NOW));
    }

    @Test
    public void rendersMoreThanOneDayWhenAtLeastADayHasElapsed() {
        Assert.assertEquals(">1d",
            SessionNewActivityStore.formatRelativeAge(NOW - ONE_DAY_MILLIS, NOW));
    }

    @Test
    public void rendersHoursJustUnderTheOneDayBoundary() {
        Assert.assertEquals("23h",
            SessionNewActivityStore.formatRelativeAge(NOW - (ONE_DAY_MILLIS - ONE_MINUTE_MILLIS), NOW));
    }
}
