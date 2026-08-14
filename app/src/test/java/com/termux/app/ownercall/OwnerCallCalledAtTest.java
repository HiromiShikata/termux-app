package com.termux.app.ownercall;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class OwnerCallCalledAtTest {

    private static final String CALLED_AT = "2026-08-14T04:22:28Z";
    private static final long CALLED_AT_EPOCH_MILLIS = 1786681348000L;

    @Test
    public void readsTheSecondPrecisionUtcInstantTheServerWrites() {
        Assert.assertEquals(Long.valueOf(CALLED_AT_EPOCH_MILLIS),
            OwnerCallCalledAt.toEpochMillis(CALLED_AT));
    }

    @Test
    public void readsNoInstantFromAnAbsentOrUnparsableCallTime() {
        Assert.assertNull(OwnerCallCalledAt.toEpochMillis(null));
        Assert.assertNull(OwnerCallCalledAt.toEpochMillis(" "));
        Assert.assertNull(OwnerCallCalledAt.toEpochMillis("yesterday"));
    }

    @Test
    public void describesHowLongTheOwnerHasBeenWaiting() {
        Assert.assertEquals("6m",
            OwnerCallCalledAt.describe(CALLED_AT, CALLED_AT_EPOCH_MILLIS + 6 * 60 * 1000L));
    }

    @Test
    public void showsTheRawCallTimeWhenItCannotBeRead() {
        Assert.assertEquals("yesterday", OwnerCallCalledAt.describe("yesterday", 0L));
        Assert.assertEquals("", OwnerCallCalledAt.describe(null, 0L));
    }
}
