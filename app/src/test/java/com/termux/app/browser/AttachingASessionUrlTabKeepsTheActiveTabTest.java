package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

public class AttachingASessionUrlTabKeepsTheActiveTabTest {

    private static final String SESSION_HANDLE = "session-handle";

    private final BrowserTabManager tabManager = new BrowserTabManager();

    @Test
    public void theTabTheUserWasOnStaysActiveWhenTheSessionUrlTabIsAttachedAgain() {
        BrowserTab sessionUrlTab = tabManager.addTab(SESSION_HANDLE, "https://example.test/session");
        BrowserTab tabTheUserOpened = tabManager.addTab(SESSION_HANDLE, "https://example.test/opened");
        tabManager.setActiveTab(tabTheUserOpened);

        tabManager.attachOrActivateTab(SESSION_HANDLE, sessionUrlTab.getUrl());

        Assert.assertSame(tabTheUserOpened, tabManager.getActiveTab(SESSION_HANDLE));
    }

    @Test
    public void aNewlyAttachedTabDoesNotTakeOverFromTheTabTheUserWasOn() {
        BrowserTab tabTheUserOpened = tabManager.addTab(SESSION_HANDLE, "https://example.test/opened");

        tabManager.attachOrActivateTab(SESSION_HANDLE, "https://example.test/session");

        Assert.assertEquals(2, tabManager.getTabs(SESSION_HANDLE).size());
        Assert.assertSame(tabTheUserOpened, tabManager.getActiveTab(SESSION_HANDLE));
    }

    @Test
    public void aSessionWithNoTabsGetsTheAttachedTabAsItsActiveTab() {
        BrowserTab attachedTab =
            tabManager.attachOrActivateTab(SESSION_HANDLE, "https://example.test/session");

        Assert.assertSame(attachedTab, tabManager.getActiveTab(SESSION_HANDLE));
    }

    @Test
    public void attachingTheSameUrlTwiceDoesNotDuplicateTheTab() {
        tabManager.attachOrActivateTab(SESSION_HANDLE, "https://example.test/session");
        tabManager.attachOrActivateTab(SESSION_HANDLE, "https://example.test/session");

        Assert.assertEquals(1, tabManager.getTabs(SESSION_HANDLE).size());
    }
}
