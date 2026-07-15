package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class BrowserSessionTabStripBindingTest {

    private static final String SESSION_A = "session-a";
    private static final String SESSION_B = "session-b";

    @Test
    public void forNullSessionProducesEmptyBinding() {
        BrowserTabManager manager = new BrowserTabManager();
        manager.addTab(SESSION_A, "https://a.example/");

        BrowserSessionTabStripBinding binding =
            BrowserSessionTabStripBinding.forSession(null, manager);

        Assert.assertTrue(binding.isEmpty());
        Assert.assertTrue(binding.getTabs().isEmpty());
        Assert.assertNull(binding.getActiveTab());
    }

    @Test
    public void forSessionProducesThatSessionsTabsAndActiveTab() {
        BrowserTabManager manager = new BrowserTabManager();
        manager.addTab(SESSION_A, "https://a.example/1");
        BrowserTab activeTabForA = manager.addTab(SESSION_A, "https://a.example/2");

        BrowserSessionTabStripBinding binding =
            BrowserSessionTabStripBinding.forSession(SESSION_A, manager);

        List<BrowserTab> tabs = binding.getTabs();
        Assert.assertEquals(2, tabs.size());
        Assert.assertEquals("https://a.example/1", tabs.get(0).getUrl());
        Assert.assertEquals("https://a.example/2", tabs.get(1).getUrl());
        Assert.assertSame(activeTabForA, binding.getActiveTab());
    }

    @Test
    public void switchingSessionRebindsToTheNewlySelectedSessionsTabSet() {
        BrowserTabManager manager = new BrowserTabManager();
        manager.addTab(SESSION_A, "https://a.example/");
        BrowserTab firstTabForB = manager.addTab(SESSION_B, "https://b.example/1");
        BrowserTab secondTabForB = manager.addTab(SESSION_B, "https://b.example/2");

        BrowserSessionTabStripBinding bindingForA =
            BrowserSessionTabStripBinding.forSession(SESSION_A, manager);
        BrowserSessionTabStripBinding bindingForB =
            BrowserSessionTabStripBinding.forSession(SESSION_B, manager);

        Assert.assertEquals(1, bindingForA.getTabs().size());
        Assert.assertEquals(2, bindingForB.getTabs().size());
        for (BrowserTab tab : bindingForB.getTabs()) {
            Assert.assertEquals(SESSION_B, tab.getSessionHandle());
        }
        Assert.assertSame(secondTabForB, bindingForB.getActiveTab());
        Assert.assertNotSame(firstTabForB, bindingForB.getActiveTab());
    }

    @Test
    public void bindingForSessionWithoutTabsIsEmptyEvenWhenAnotherSessionHasTabs() {
        BrowserTabManager manager = new BrowserTabManager();
        manager.addTab(SESSION_A, "https://a.example/");

        BrowserSessionTabStripBinding binding =
            BrowserSessionTabStripBinding.forSession(SESSION_B, manager);

        Assert.assertTrue(binding.isEmpty());
        Assert.assertNull(binding.getActiveTab());
    }

    @Test
    public void bindingSnapshotIsNotAffectedByLaterMutationOfTheManager() {
        BrowserTabManager manager = new BrowserTabManager();
        manager.addTab(SESSION_A, "https://a.example/1");

        BrowserSessionTabStripBinding binding =
            BrowserSessionTabStripBinding.forSession(SESSION_A, manager);
        manager.addTab(SESSION_A, "https://a.example/2");

        Assert.assertEquals(1, binding.getTabs().size());
    }
}
