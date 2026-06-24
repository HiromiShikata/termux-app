package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class BrowserTabManagerRestoreTest {

    private static final String SESSION_A = "session-a";
    private static final String SESSION_B = "session-b";

    @Test
    public void restoreTabsSeedsTabsWithUrlTitleDesktopModeAndOrder() {
        BrowserTabManager manager = new BrowserTabManager();
        List<BrowserPersistedTab> persistedTabs = Arrays.asList(
            new BrowserPersistedTab("https://a.example/1", "First", true),
            new BrowserPersistedTab("https://a.example/2", "Second", false));

        manager.restoreTabs(SESSION_A, persistedTabs, 1);

        List<BrowserTab> tabs = manager.getTabs(SESSION_A);
        Assert.assertEquals(2, tabs.size());
        Assert.assertEquals("https://a.example/1", tabs.get(0).getUrl());
        Assert.assertEquals("First", tabs.get(0).getTitle());
        Assert.assertTrue(tabs.get(0).isDesktopMode());
        Assert.assertEquals("https://a.example/2", tabs.get(1).getUrl());
        Assert.assertFalse(tabs.get(1).isDesktopMode());
        Assert.assertSame(tabs.get(1), manager.getActiveTab(SESSION_A));
    }

    @Test
    public void restoreTabsDoesNotOverwriteExistingTabsForSession() {
        BrowserTabManager manager = new BrowserTabManager();
        BrowserTab liveTab = manager.addTab(SESSION_A, "https://live.example/");

        manager.restoreTabs(SESSION_A,
            Arrays.asList(new BrowserPersistedTab("https://persisted.example/", "Persisted", true)), 0);

        Assert.assertEquals(1, manager.getTabs(SESSION_A).size());
        Assert.assertSame(liveTab, manager.getTabs(SESSION_A).get(0));
    }

    @Test
    public void restoreTabsIsScopedPerSessionAndNeverLeaksAcrossSessions() {
        BrowserTabManager manager = new BrowserTabManager();
        manager.restoreTabs(SESSION_A,
            Arrays.asList(new BrowserPersistedTab("https://a.example/", "A", true)), 0);
        manager.restoreTabs(SESSION_B,
            Arrays.asList(
                new BrowserPersistedTab("https://b1.example/", "B1", true),
                new BrowserPersistedTab("https://b2.example/", "B2", true)),
            0);

        Assert.assertEquals(1, manager.getTabs(SESSION_A).size());
        Assert.assertEquals(2, manager.getTabs(SESSION_B).size());
        for (BrowserTab tab : manager.getTabs(SESSION_A)) {
            Assert.assertEquals(SESSION_A, tab.getSessionHandle());
        }
        for (BrowserTab tab : manager.getTabs(SESSION_B)) {
            Assert.assertEquals(SESSION_B, tab.getSessionHandle());
        }
    }

    @Test
    public void restoreTabsWithEmptyListLeavesSessionWithoutTabs() {
        BrowserTabManager manager = new BrowserTabManager();
        manager.restoreTabs(SESSION_A, Collections.emptyList(), 0);

        Assert.assertTrue(manager.getTabs(SESSION_A).isEmpty());
        Assert.assertNull(manager.getActiveTab(SESSION_A));
    }

    @Test
    public void restoreTabsClampsOutOfRangeActiveIndexToLastTab() {
        BrowserTabManager manager = new BrowserTabManager();
        manager.restoreTabs(SESSION_A,
            Arrays.asList(
                new BrowserPersistedTab("https://a.example/1", "1", true),
                new BrowserPersistedTab("https://a.example/2", "2", true)),
            9);

        Assert.assertSame(manager.getTabs(SESSION_A).get(1), manager.getActiveTab(SESSION_A));
    }

    @Test
    public void activeTabIndexReflectsActiveSelection() {
        BrowserTabManager manager = new BrowserTabManager();
        BrowserTab first = manager.addTab(SESSION_A, "https://a.example/1");
        manager.addTab(SESSION_A, "https://a.example/2");

        manager.setActiveTab(first);

        Assert.assertEquals(0, manager.getActiveTabIndex(SESSION_A));
    }

    @Test
    public void hasTabsReportsPresenceOfTabsForSession() {
        BrowserTabManager manager = new BrowserTabManager();
        Assert.assertFalse(manager.hasTabs(SESSION_A));
        manager.addTab(SESSION_A, "https://a.example/");
        Assert.assertTrue(manager.hasTabs(SESSION_A));
    }
}
