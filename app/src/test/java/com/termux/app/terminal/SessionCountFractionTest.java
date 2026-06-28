package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class SessionCountFractionTest {

    @Test
    public void formatsCallSessionsOverTotalInParentheses() {
        Assert.assertEquals("(3/25)", SessionCountFraction.of(3, 25));
    }

    @Test
    public void formatsZeroCallSessions() {
        Assert.assertEquals("(0/4)", SessionCountFraction.of(0, 4));
    }

    @Test
    public void formatsZeroOverZeroWhenThereAreNoSessions() {
        Assert.assertEquals("(0/0)", SessionCountFraction.of(0, 0));
    }
}
