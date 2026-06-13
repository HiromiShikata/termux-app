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
    public void oneSessionTabListNeverContainsAnotherSessionTabs() {
        BrowserTabManager manager = new BrowserTabManager();
        for (int i = 0; i < 5; i++) manager.addTab(SESSION_A, "https://a.example/" + i);
        for (int i = 0; i < 3; i++) manager.addTab(SESSION_B, "https://b.example/" + i);

        Assert.assertEquals(5, manager.getTabs(SESSION_A).size());
        Assert.assertEquals(3, manager.getTabs(SESSION_B).size());
        for (BrowserTab tab : manager.getTabs(SESSION_A)) {
            Assert.assertEquals(SESSION_A, tab.getSessionHandle());
        }
        for (BrowserTab tab : manager.getTabs(SESSION_B)) {
            Assert.assertEquals(SESSION_B, tab.getSessionHandle());
        }
    }

    @Test
    public void activeTabIsScopedPerSession() {
        BrowserTabManager manager = new BrowserTabManager();
        BrowserTab tabInSessionA = manager.addTab(SESSION_A, "https://a.example/");
        BrowserTab tabInSessionB = manager.addTab(SESSION_B, "https://b.example/");

        Assert.assertSame(tabInSessionA, manager.getActiveTab(SESSION_A));
        Assert.assertSame(tabInSessionB, manager.getActiveTab(SESSION_B));
        Assert.assertNotSame(manager.getActiveTab(SESSION_A), manager.getActiveTab(SESSION_B));
    }

    @Test
    public void addedTabBecomesActiveTab() {
        BrowserTabManager manager = new BrowserTabManager();
        BrowserTab tab = manager.addTab(SESSION_A, "https://a.example/");
        Assert.assertSame(tab, manager.getActiveTab(SESSION_A));
    }

    @Test
    public void manyTabsCanBeOpenedWithoutCap() {
        BrowserTabManager manager = new BrowserTabManager();
        int tabCount = 100;
        for (int i = 0; i < tabCount; i++) {
            Assert.assertNotNull(manager.addTab(SESSION_A, "https://a.example/" + i));
        }
        Assert.assertEquals(tabCount, manager.getTabs(SESSION_A).size());
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

    @Test
    public void removingSessionDoesNotAffectOtherSessions() {
        BrowserTabManager manager = new BrowserTabManager();
        manager.addTab(SESSION_A, "https://a.example/1");
        manager.addTab(SESSION_A, "https://a.example/2");
        BrowserTab tabInSessionB = manager.addTab(SESSION_B, "https://b.example/");

        manager.removeSession(SESSION_A);

        Assert.assertTrue(manager.getTabs(SESSION_A).isEmpty());
        Assert.assertEquals(1, manager.getTabs(SESSION_B).size());
        Assert.assertSame(tabInSessionB, manager.getActiveTab(SESSION_B));
    }
}
