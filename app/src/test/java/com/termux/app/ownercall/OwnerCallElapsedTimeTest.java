package com.termux.app.ownercall;

import org.junit.Assert;
import org.junit.Test;

public class OwnerCallElapsedTimeTest {

    private static final long ONE_SECOND_MILLIS = 1000L;
    private static final long ONE_MINUTE_MILLIS = 60L * ONE_SECOND_MILLIS;
    private static final long ONE_HOUR_MILLIS = 60L * ONE_MINUTE_MILLIS;
    private static final long CALLED_AT_MILLIS = 1_800_000_000_000L;

    @Test
    public void countsInSecondsWithinTheFirstMinute() {
        Assert.assertEquals("0s", OwnerCallElapsedTime.of(CALLED_AT_MILLIS, CALLED_AT_MILLIS));
        Assert.assertEquals("59s",
            OwnerCallElapsedTime.of(CALLED_AT_MILLIS, CALLED_AT_MILLIS + 59 * ONE_SECOND_MILLIS));
    }

    @Test
    public void countsInMinutesWithinTheFirstHour() {
        Assert.assertEquals("1m",
            OwnerCallElapsedTime.of(CALLED_AT_MILLIS, CALLED_AT_MILLIS + ONE_MINUTE_MILLIS));
        Assert.assertEquals("59m",
            OwnerCallElapsedTime.of(CALLED_AT_MILLIS, CALLED_AT_MILLIS + 59 * ONE_MINUTE_MILLIS));
    }

    @Test
    public void countsInHoursBeyondTheFirstHourHoweverLongTheOwnerWasAway() {
        Assert.assertEquals("1h",
            OwnerCallElapsedTime.of(CALLED_AT_MILLIS, CALLED_AT_MILLIS + ONE_HOUR_MILLIS));
        Assert.assertEquals("30h",
            OwnerCallElapsedTime.of(CALLED_AT_MILLIS, CALLED_AT_MILLIS + 30 * ONE_HOUR_MILLIS));
    }

    @Test
    public void showsNoElapsedTimeInTheFutureWhenTheDeviceClockLagsTheCallTime() {
        Assert.assertEquals("0s",
            OwnerCallElapsedTime.of(CALLED_AT_MILLIS, CALLED_AT_MILLIS - ONE_HOUR_MILLIS));
    }
}
