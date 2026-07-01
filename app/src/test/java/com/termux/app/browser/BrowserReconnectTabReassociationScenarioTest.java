package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BrowserReconnectTabReassociationScenarioTest {

    private static final String SESSION_NAME = "worker-session";
    private static final String OLD_HANDLE = "handle-old";
    private static final String NEW_HANDLE = "handle-new";

    @Test
    public void reconnectPreservesPersistedTabsAndReassociatesThemToTheNewHandle() {
        BrowserTabManager tabManager = new BrowserTabManager();
        Map<String, BrowserPersistedSessionTabs> persistedTabsBySessionName = new LinkedHashMap<>();

        seedTabsForHandle(tabManager, OLD_HANDLE, "https://tab1.example/", "https://tab2.example/");
        persistTabs(tabManager, persistedTabsBySessionName, OLD_HANDLE);

        removeSession(tabManager, persistedTabsBySessionName, OLD_HANDLE,
            BrowserSessionRemovalReason.RECONNECT);

        Assert.assertFalse("persisted tabs must survive a reconnect-driven removal",
            persistedTabsBySessionName.isEmpty());
        Assert.assertFalse(tabManager.hasTabs(OLD_HANDLE));

        restoreForHandle(tabManager, persistedTabsBySessionName, NEW_HANDLE);

        List<BrowserTab> reassociatedTabs = tabManager.getTabs(NEW_HANDLE);
        Assert.assertEquals(2, reassociatedTabs.size());
        Assert.assertEquals("https://tab1.example/", reassociatedTabs.get(0).getUrl());
        Assert.assertEquals("https://tab2.example/", reassociatedTabs.get(1).getUrl());
        for (BrowserTab tab : reassociatedTabs) {
            Assert.assertEquals(NEW_HANDLE, tab.getSessionHandle());
        }
    }

    @Test
    public void genuineUserCloseClearsPersistedTabsAndLeavesNothingToReassociate() {
        BrowserTabManager tabManager = new BrowserTabManager();
        Map<String, BrowserPersistedSessionTabs> persistedTabsBySessionName = new LinkedHashMap<>();

        seedTabsForHandle(tabManager, OLD_HANDLE, "https://tab1.example/", "https://tab2.example/");
        persistTabs(tabManager, persistedTabsBySessionName, OLD_HANDLE);

        removeSession(tabManager, persistedTabsBySessionName, OLD_HANDLE,
            BrowserSessionRemovalReason.USER_CLOSE);

        Assert.assertTrue("persisted tabs must be cleared on a genuine user close",
            persistedTabsBySessionName.isEmpty());

        restoreForHandle(tabManager, persistedTabsBySessionName, NEW_HANDLE);

        Assert.assertFalse(tabManager.hasTabs(NEW_HANDLE));
    }

    private static void seedTabsForHandle(BrowserTabManager tabManager, String handle, String... urls) {
        for (String url : urls) {
            tabManager.addTab(handle, url);
        }
    }

    private static void persistTabs(BrowserTabManager tabManager,
                                    Map<String, BrowserPersistedSessionTabs> persistedTabsBySessionName,
                                    String handle) {
        List<BrowserTab> tabs = tabManager.getTabs(handle);
        List<BrowserPersistedTab> persistedTabs = new ArrayList<>();
        for (BrowserTab tab : tabs) {
            persistedTabs.add(new BrowserPersistedTab(tab.getUrl(), tab.getTitle(), tab.getViewMode().isDesktop()));
        }
        persistedTabsBySessionName.put(SESSION_NAME,
            new BrowserPersistedSessionTabs(SESSION_NAME, persistedTabs,
                Math.max(tabManager.getActiveTabIndex(handle), 0)));
    }

    private static void removeSession(BrowserTabManager tabManager,
                                      Map<String, BrowserPersistedSessionTabs> persistedTabsBySessionName,
                                      String handle,
                                      BrowserSessionRemovalReason reason) {
        tabManager.removeSession(handle);
        if (BrowserSessionRemovalTabRetention.shouldDeletePersistedTabs(reason)) {
            persistedTabsBySessionName.remove(SESSION_NAME);
        }
    }

    private static void restoreForHandle(BrowserTabManager tabManager,
                                         Map<String, BrowserPersistedSessionTabs> persistedTabsBySessionName,
                                         String handle) {
        BrowserPersistedSessionTabs persistedSessionTabs = persistedTabsBySessionName.get(SESSION_NAME);
        if (persistedSessionTabs == null) return;
        tabManager.restoreTabs(handle, persistedSessionTabs.getTabs(), persistedSessionTabs.getActiveTabIndex());
    }
}
