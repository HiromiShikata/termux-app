package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

public class BrowserHistoryIsolationTest {

    @Test
    public void aCrossTabSwitchClearsTheSharedBackForwardList() {
        BrowserHistoryIsolation isolation = BrowserHistoryIsolation.resolve(true);

        Assert.assertTrue(isolation.shouldClearHistory());
    }

    @Test
    public void aSameTabRedisplayPreservesTheBackForwardList() {
        BrowserHistoryIsolation isolation = BrowserHistoryIsolation.resolve(false);

        Assert.assertFalse(isolation.shouldClearHistory());
    }
}
