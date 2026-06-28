package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class CallToUserScrollActionTest {

    @Test
    public void scrollsToTagWhenMainBufferActiveAndTagLocated() {
        CallToUserScrollAction action = CallToUserScrollAction.resolve(false, -7);

        Assert.assertEquals(CallToUserScrollAction.Kind.SCROLL_TO_TAG, action.getKind());
        Assert.assertEquals(-7, action.getTargetTopRow());
    }

    @Test
    public void scrollsToLatestOutputWhenAlternateBufferActiveSoNoPtyCommandIsNeeded() {
        CallToUserScrollAction action = CallToUserScrollAction.resolve(true, -7);

        Assert.assertEquals(CallToUserScrollAction.Kind.SCROLL_TO_LATEST, action.getKind());
        Assert.assertEquals(CallToUserScrollAction.LATEST_OUTPUT_TOP_ROW, action.getTargetTopRow());
    }

    @Test
    public void scrollsToLatestOutputWhenTagNotFoundInMainBuffer() {
        CallToUserScrollAction action = CallToUserScrollAction.resolve(
            false, CallToUserTagScrollLocator.NO_TAG_ROW);

        Assert.assertEquals(CallToUserScrollAction.Kind.SCROLL_TO_LATEST, action.getKind());
        Assert.assertEquals(CallToUserScrollAction.LATEST_OUTPUT_TOP_ROW, action.getTargetTopRow());
    }

    @Test
    public void latestOutputTopRowIsZeroWhichDisplaysTheNewestOutput() {
        Assert.assertEquals(0, CallToUserScrollAction.LATEST_OUTPUT_TOP_ROW);
    }
}
