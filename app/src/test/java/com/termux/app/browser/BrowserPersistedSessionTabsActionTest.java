package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

public class BrowserPersistedSessionTabsActionTest {

    @Test
    public void aSessionThatHasTabsInMemoryIsWrittenFromMemory() {
        Assert.assertEquals(BrowserPersistedSessionTabsAction.REWRITE_FROM_MEMORY,
            BrowserPersistedSessionTabsAction.decide(true, true));
    }

    @Test
    public void aSessionWhoseLoadedTabsAreAllGoneHasItsStoredTabsRemoved() {
        Assert.assertEquals(BrowserPersistedSessionTabsAction.REMOVE,
            BrowserPersistedSessionTabsAction.decide(true, false));
    }

    @Test
    public void aSessionWhoseStoredTabsWereNeverLoadedKeepsThem() {
        Assert.assertEquals(BrowserPersistedSessionTabsAction.KEEP_PERSISTED,
            BrowserPersistedSessionTabsAction.decide(false, false));
    }

    @Test
    public void aSessionThatAlreadyHasTabsIsWrittenFromMemoryEvenBeforeItsStoredTabsWereLoaded() {
        Assert.assertEquals(BrowserPersistedSessionTabsAction.REWRITE_FROM_MEMORY,
            BrowserPersistedSessionTabsAction.decide(false, true));
    }
}
