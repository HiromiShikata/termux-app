package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class CallToUserScrollDecisionTest {

    @Test
    public void disallowsInAppScrollWhenAlternateBufferActive() {
        Assert.assertFalse(CallToUserScrollDecision.allowsInAppScroll(true));
    }

    @Test
    public void allowsInAppScrollWhenAlternateBufferInactive() {
        Assert.assertTrue(CallToUserScrollDecision.allowsInAppScroll(false));
    }
}
