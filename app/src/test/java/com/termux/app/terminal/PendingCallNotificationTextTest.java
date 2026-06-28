package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class PendingCallNotificationTextTest {

    @Test
    public void formatsPendingCallsOverTotalWithCallsLabel() {
        Assert.assertEquals("3/25 calls", PendingCallNotificationText.fractionSuffix(3, 25));
    }

    @Test
    public void formatsZeroPendingCalls() {
        Assert.assertEquals("0/4 calls", PendingCallNotificationText.fractionSuffix(0, 4));
    }

    @Test
    public void returnsEmptyWhenThereAreNoSessions() {
        Assert.assertEquals("", PendingCallNotificationText.fractionSuffix(0, 0));
    }

    @Test
    public void returnsEmptyWhenTotalIsNegative() {
        Assert.assertEquals("", PendingCallNotificationText.fractionSuffix(2, -1));
    }

    @Test
    public void clampsPendingCountToTotal() {
        Assert.assertEquals("4/4 calls", PendingCallNotificationText.fractionSuffix(9, 4));
    }

    @Test
    public void clampsNegativePendingCountToZero() {
        Assert.assertEquals("0/4 calls", PendingCallNotificationText.fractionSuffix(-3, 4));
    }
}
