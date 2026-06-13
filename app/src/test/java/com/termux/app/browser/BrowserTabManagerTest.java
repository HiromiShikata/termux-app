package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

public class BrowserTabManagerTest {

    private static final String SESSION_A = "session-a";
    private static final String SESSION_B = "session-b";

    @Test
    public void tabsAreScopedPerSession() {
        BrowserTabManager manager = new BrowserTabManager();
        manager.addTab(SESSION_A, "https://a.example/");
        manager.addTab(SESSION_B, "https://b.example/");

        Assert.assertEquals(1, manager.getTabs(SESSION_A).size());
        Assert.assertEquals(1, manager.getTabs(SESSION_B).size());
        Assert.assertEquals(SESSION_A, manager.getTabs(SESSION_A).get(0).getSessionHandle());
    }

    @Test
    public void addedTabBecomesActiveTab() {
        BrowserTabManager manager = new BrowserTabManager();
        BrowserTab tab = manager.addTab(SESSION_A, "https://a.example/");
        Assert.assertSame(tab, manager.getActiveTab(SESSION_A));
    }

    @Test
    public void perSessionTabCountIsCappedAtMaximum() {
        BrowserTabManager manager = new BrowserTabManager();
        for (int i = 0; i < BrowserTabManager.MAX_TABS_PER_SESSION; i++) {
            Assert.assertNotNull(manager.addTab(SESSION_A, "https://a.example/" + i));
        }
        Assert.assertFalse(manager.canAddTab(SESSION_A));
        Assert.assertNull(manager.addTab(SESSION_A, "https://a.example/overflow"));
        Assert.assertEquals(BrowserTabManager.MAX_TABS_PER_SESSION, manager.getTabs(SESSION_A).size());
    }

    @Test
    public void findTabByUrlReturnsMatchingTab() {
        BrowserTabManager manager = new BrowserTabManager();
        manager.addTab(SESSION_A, "https://a.example/1");
        BrowserTab second = manager.addTab(SESSION_A, "https://a.example/2");

        Assert.assertSame(second, manager.findTabByUrl(SESSION_A, "https://a.example/2"));
    }

    @Test
    public void findTabByUrlReturnsNullWhenUrlAbsent() {
        BrowserTabManager manager = new BrowserTabManager();
        manager.addTab(SESSION_A, "https://a.example/1");

        Assert.assertNull(manager.findTabByUrl(SESSION_A, "https://a.example/missing"));
    }

    @Test
    public void findTabByUrlIsScopedPerSession() {
        BrowserTabManager manager = new BrowserTabManager();
        manager.addTab(SESSION_A, "https://shared.example/");

        Assert.assertNotNull(manager.findTabByUrl(SESSION_A, "https://shared.example/"));
        Assert.assertNull(manager.findTabByUrl(SESSION_B, "https://shared.example/"));
    }

    @Test
    public void removingActiveTabSelectsAdjacentTab() {
        BrowserTabManager manager = new BrowserTabManager();
        BrowserTab first = manager.addTab(SESSION_A, "https://a.example/1");
        BrowserTab second = manager.addTab(SESSION_A, "https://a.example/2");

        manager.setActiveTab(first);
        manager.removeTab(first);

        Assert.assertSame(second, manager.getActiveTab(SESSION_A));
        Assert.assertEquals(1, manager.getTabs(SESSION_A).size());
    }

    @Test
    public void removingLastTabClearsActiveTab() {
        BrowserTabManager manager = new BrowserTabManager();
        BrowserTab tab = manager.addTab(SESSION_A, "https://a.example/");
        manager.removeTab(tab);
        Assert.assertNull(manager.getActiveTab(SESSION_A));
        Assert.assertTrue(manager.getTabs(SESSION_A).isEmpty());
    }

    @Test
    public void removingSessionDiscardsItsTabs() {
        BrowserTabManager manager = new BrowserTabManager();
        manager.addTab(SESSION_A, "https://a.example/");
        manager.removeSession(SESSION_A);
        Assert.assertTrue(manager.getTabs(SESSION_A).isEmpty());
        Assert.assertNull(manager.getActiveTab(SESSION_A));
    }
}
