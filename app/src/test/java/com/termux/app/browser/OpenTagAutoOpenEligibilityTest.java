package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

public class OpenTagAutoOpenEligibilityTest {

    private static final long SEEN_AT = 1_000_000L;

    @Test
    public void outputThatArrivedAfterTheOwnerLastSawTheSessionHasNotBeenOpenedYet() {
        Assert.assertTrue("the owner last looked at this session before this output arrived, so the"
                + " tag this output carries has not been auto-opened for him yet",
            OpenTagAutoOpenEligibility.shouldAutoOpen(SEEN_AT, SEEN_AT + 1));
    }

    @Test
    public void outputTheOwnerHasAlreadySeenHasAlreadyBeenOpenedOnce() {
        Assert.assertFalse("the owner has looked at this session since this output arrived, so its"
                + " tag was already auto-opened once and opening it again is the repeat he reported",
            OpenTagAutoOpenEligibility.shouldAutoOpen(SEEN_AT + 1, SEEN_AT));
    }

    @Test
    public void outputSeenInTheSameMillisecondCountsAsAlreadySeen() {
        Assert.assertFalse(OpenTagAutoOpenEligibility.shouldAutoOpen(SEEN_AT, SEEN_AT));
    }

    @Test
    public void aSessionTheOwnerHasNeverSeenStillOpensItsOutput() {
        Assert.assertTrue("a session with no recorded seen time has never been looked at, so its"
                + " output has never been auto-opened",
            OpenTagAutoOpenEligibility.shouldAutoOpen(null, SEEN_AT));
    }

    @Test
    public void withoutAnOutputTimeThereIsNothingToCallUnseen() {
        Assert.assertFalse(OpenTagAutoOpenEligibility.shouldAutoOpen(SEEN_AT, null));
        Assert.assertFalse(OpenTagAutoOpenEligibility.shouldAutoOpen(null, null));
    }
}
