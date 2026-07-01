package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

public class BrowserSessionRemovalTabRetentionTest {

    @Test
    public void genuineUserCloseDeletesPersistedTabs() {
        Assert.assertTrue(
            BrowserSessionRemovalTabRetention.shouldDeletePersistedTabs(BrowserSessionRemovalReason.USER_CLOSE));
    }

    @Test
    public void reconnectDrivenRemovalPreservesPersistedTabs() {
        Assert.assertFalse(
            BrowserSessionRemovalTabRetention.shouldDeletePersistedTabs(BrowserSessionRemovalReason.RECONNECT));
    }
}
