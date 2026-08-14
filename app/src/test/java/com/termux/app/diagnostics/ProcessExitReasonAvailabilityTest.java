package com.termux.app.diagnostics;

import org.junit.Assert;
import org.junit.Test;

public class ProcessExitReasonAvailabilityTest {

    @Test
    public void theVersionThatIntroducedTheRecordSuppliesIt() {
        Assert.assertTrue("Android 11 is where the system began keeping why each process of an app ended,"
                + " and a reader on that version must get the section rather than a refusal",
            ProcessExitReasonAvailability.isRecordedBy(30));
    }

    @Test
    public void aLaterVersionSuppliesItToo() {
        Assert.assertTrue("every version after the one that introduced the record keeps it",
            ProcessExitReasonAvailability.isRecordedBy(34));
    }

    @Test
    public void anEarlierVersionDoesNotSupplyIt() {
        Assert.assertFalse("asking an older system for the record throws, so the reading must not be"
                + " attempted there",
            ProcessExitReasonAvailability.isRecordedBy(29));
    }
}
