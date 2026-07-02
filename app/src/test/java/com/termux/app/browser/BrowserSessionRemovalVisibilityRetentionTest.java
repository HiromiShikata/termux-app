package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

public class BrowserSessionRemovalVisibilityRetentionTest {

    @Test
    public void genuineUserCloseClearsTheBrowserOpenSessionName() {
        Assert.assertTrue(
            BrowserSessionRemovalVisibilityRetention.shouldClearBrowserOpenSessionName(
                BrowserSessionRemovalReason.USER_CLOSE));
    }

    @Test
    public void reconnectDrivenRemovalPreservesTheBrowserOpenSessionName() {
        Assert.assertFalse(
            BrowserSessionRemovalVisibilityRetention.shouldClearBrowserOpenSessionName(
                BrowserSessionRemovalReason.RECONNECT));
    }
}
