package com.termux.app.ownercall;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class OwnerCallCalledAtTest {

    private static final long CALLED_AT_EPOCH_MILLIS = 1_800_000_000_000L;
    private static final String CALLED_AT = "2027-01-15T08:00:00.000Z";

    @Test
    public void readsTheCallTimeWrittenBySessionDefinitionDocuments() {
        assertEquals(Long.valueOf(CALLED_AT_EPOCH_MILLIS), OwnerCallCalledAt.toEpochMillis(CALLED_AT));
    }

    @Test
    public void readsACallTimeThatCarriesAZoneOffsetInsteadOfZulu() {
        assertEquals(Long.valueOf(CALLED_AT_EPOCH_MILLIS),
            OwnerCallCalledAt.toEpochMillis("2027-01-15T17:00:00+09:00"));
    }

    @Test
    public void reportsNoTimeForAnAbsentOrUnparsableCallTime() {
        assertNull(OwnerCallCalledAt.toEpochMillis(null));
        assertNull(OwnerCallCalledAt.toEpochMillis(""));
        assertNull(OwnerCallCalledAt.toEpochMillis("   "));
        assertNull(OwnerCallCalledAt.toEpochMillis("yesterday"));
    }

    @Test
    public void describesTheCallTimeAsElapsedTime() {
        assertEquals("6分前",
            OwnerCallCalledAt.describe(CALLED_AT, CALLED_AT_EPOCH_MILLIS + 6 * 60 * 1000L));
    }

    @Test
    public void showsTheRawCallTimeWhenItCannotBeRead() {
        assertEquals("yesterday", OwnerCallCalledAt.describe("yesterday", CALLED_AT_EPOCH_MILLIS));
    }
}
