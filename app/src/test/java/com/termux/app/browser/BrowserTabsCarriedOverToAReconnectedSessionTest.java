package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

public class BrowserTabsCarriedOverToAReconnectedSessionTest {

    private static final String SESSION_BEFORE_RECONNECT = "session-handle-before-reconnect";

    private static final String SESSION_AFTER_RECONNECT = "session-handle-after-reconnect";

    @Test
    public void theCarriedOverTabsAreTheSameInstancesSoTheirWebViewsAreNotRebuilt() {
        BrowserTabManager manager = new BrowserTabManager();
        BrowserTab firstTab = manager.addTab(SESSION_BEFORE_RECONNECT, "https://example.com/first");
        BrowserTab secondTab = manager.addTab(SESSION_BEFORE_RECONNECT, "https://example.com/second");

        manager.moveSession(SESSION_BEFORE_RECONNECT, SESSION_AFTER_RECONNECT);

        Assert.assertEquals(2, manager.getTabs(SESSION_AFTER_RECONNECT).size());
        Assert.assertSame(firstTab, manager.getTabs(SESSION_AFTER_RECONNECT).get(0));
        Assert.assertSame(secondTab, manager.getTabs(SESSION_AFTER_RECONNECT).get(1));
        Assert.assertFalse(manager.hasTabs(SESSION_BEFORE_RECONNECT));
    }

    @Test
    public void theCarriedOverTabsBelongToTheReconnectedSession() {
        BrowserTabManager manager = new BrowserTabManager();
        BrowserTab tab = manager.addTab(SESSION_BEFORE_RECONNECT, "https://example.com/first");

        manager.moveSession(SESSION_BEFORE_RECONNECT, SESSION_AFTER_RECONNECT);

        Assert.assertEquals(SESSION_AFTER_RECONNECT, tab.getSessionHandle());
    }

    @Test
    public void theTabTheUserWasOnStaysActiveAcrossTheReconnect() {
        BrowserTabManager manager = new BrowserTabManager();
        manager.addTab(SESSION_BEFORE_RECONNECT, "https://example.com/first");
        BrowserTab readTab = manager.addTab(SESSION_BEFORE_RECONNECT, "https://example.com/second");
        manager.setActiveTab(readTab);

        manager.moveSession(SESSION_BEFORE_RECONNECT, SESSION_AFTER_RECONNECT);

        Assert.assertSame(readTab, manager.getActiveTab(SESSION_AFTER_RECONNECT));
    }

    @Test
    public void aSessionWithNoTabsCarriesNothingOver() {
        BrowserTabManager manager = new BrowserTabManager();

        manager.moveSession(SESSION_BEFORE_RECONNECT, SESSION_AFTER_RECONNECT);

        Assert.assertFalse(manager.hasTabs(SESSION_AFTER_RECONNECT));
    }

    @Test
    public void onlyAReconnectKeepsTheLiveTabsOfTheRemovedSession() {
        Assert.assertTrue(BrowserSessionRemovalLiveTabRetention.shouldKeepLiveTabs(
            BrowserSessionRemovalReason.RECONNECT));
        Assert.assertFalse(BrowserSessionRemovalLiveTabRetention.shouldKeepLiveTabs(
            BrowserSessionRemovalReason.USER_CLOSE));
    }

    @Test
    public void anOpenBrowserStaysOpenForTheReconnectedSession() {
        BrowserSessionVisibilityState visibilityState = new BrowserSessionVisibilityState();
        visibilityState.setBrowserVisible(SESSION_BEFORE_RECONNECT, true);

        visibilityState.moveSession(SESSION_BEFORE_RECONNECT, SESSION_AFTER_RECONNECT);

        Assert.assertTrue(visibilityState.wasBrowserVisible(SESSION_AFTER_RECONNECT));
        Assert.assertFalse(visibilityState.wasBrowserVisible(SESSION_BEFORE_RECONNECT));
    }

    @Test
    public void aClosedBrowserStaysClosedForTheReconnectedSession() {
        BrowserSessionVisibilityState visibilityState = new BrowserSessionVisibilityState();

        visibilityState.moveSession(SESSION_BEFORE_RECONNECT, SESSION_AFTER_RECONNECT);

        Assert.assertFalse(visibilityState.wasBrowserVisible(SESSION_AFTER_RECONNECT));
    }
}
