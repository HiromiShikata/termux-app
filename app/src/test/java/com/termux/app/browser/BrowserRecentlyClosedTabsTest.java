package com.termux.app.browser;

import androidx.annotation.NonNull;

import org.junit.Assert;
import org.junit.Test;

public class BrowserRecentlyClosedTabsTest {

    private static final String SESSION_A = "session-a";

    private static BrowserClosedTab closedTab(@NonNull String url, int listIndex) {
        return new BrowserClosedTab(SESSION_A, url, url, false, listIndex);
    }

    @Test
    public void newStackIsEmpty() {
        BrowserRecentlyClosedTabs stack = new BrowserRecentlyClosedTabs();
        Assert.assertTrue(stack.isEmpty());
        Assert.assertEquals(0, stack.size());
    }

    @Test
    public void popOnEmptyStackReturnsNull() {
        BrowserRecentlyClosedTabs stack = new BrowserRecentlyClosedTabs();
        Assert.assertNull(stack.pop());
    }

    @Test
    public void peekOnEmptyStackReturnsNull() {
        BrowserRecentlyClosedTabs stack = new BrowserRecentlyClosedTabs();
        Assert.assertNull(stack.peek());
    }

    @Test
    public void pushMakesStackNonEmpty() {
        BrowserRecentlyClosedTabs stack = new BrowserRecentlyClosedTabs();
        stack.push(closedTab("https://a.example/", 0));
        Assert.assertFalse(stack.isEmpty());
        Assert.assertEquals(1, stack.size());
    }

    @Test
    public void popReturnsMostRecentlyPushedFirst() {
        BrowserRecentlyClosedTabs stack = new BrowserRecentlyClosedTabs();
        stack.push(closedTab("https://first.example/", 0));
        stack.push(closedTab("https://second.example/", 1));
        stack.push(closedTab("https://third.example/", 2));

        Assert.assertEquals("https://third.example/", stack.pop().getUrl());
        Assert.assertEquals("https://second.example/", stack.pop().getUrl());
        Assert.assertEquals("https://first.example/", stack.pop().getUrl());
        Assert.assertTrue(stack.isEmpty());
    }

    @Test
    public void peekReturnsMostRecentWithoutRemoving() {
        BrowserRecentlyClosedTabs stack = new BrowserRecentlyClosedTabs();
        stack.push(closedTab("https://first.example/", 0));
        stack.push(closedTab("https://second.example/", 1));

        Assert.assertEquals("https://second.example/", stack.peek().getUrl());
        Assert.assertEquals(2, stack.size());
    }

    @Test
    public void stackIsBoundedToMaxSizeDroppingOldest() {
        BrowserRecentlyClosedTabs stack = new BrowserRecentlyClosedTabs(3);
        stack.push(closedTab("https://one.example/", 0));
        stack.push(closedTab("https://two.example/", 1));
        stack.push(closedTab("https://three.example/", 2));
        stack.push(closedTab("https://four.example/", 3));

        Assert.assertEquals(3, stack.size());
        Assert.assertEquals("https://four.example/", stack.pop().getUrl());
        Assert.assertEquals("https://three.example/", stack.pop().getUrl());
        Assert.assertEquals("https://two.example/", stack.pop().getUrl());
        Assert.assertTrue(stack.isEmpty());
    }

    @Test
    public void defaultMaxSizeIsTen() {
        BrowserRecentlyClosedTabs stack = new BrowserRecentlyClosedTabs();
        Assert.assertEquals(BrowserRecentlyClosedTabs.DEFAULT_MAX_SIZE, stack.getMaxSize());
        Assert.assertEquals(10, stack.getMaxSize());

        for (int index = 0; index < 15; index++) {
            stack.push(closedTab("https://example.com/" + index, index));
        }
        Assert.assertEquals(10, stack.size());
        Assert.assertEquals("https://example.com/14", stack.peek().getUrl());
    }

    @Test(expected = IllegalArgumentException.class)
    public void maxSizeBelowOneIsRejected() {
        new BrowserRecentlyClosedTabs(0);
    }

    @Test
    public void closedTabPreservesCapturedState() {
        BrowserClosedTab closedTab =
            new BrowserClosedTab(SESSION_A, "https://a.example/", "Example Title", true, 4);
        Assert.assertEquals(SESSION_A, closedTab.getSessionHandle());
        Assert.assertEquals("https://a.example/", closedTab.getUrl());
        Assert.assertEquals("Example Title", closedTab.getTitle());
        Assert.assertTrue(closedTab.isDesktopMode());
        Assert.assertEquals(4, closedTab.getListIndex());
    }

    @Test
    public void removeSessionDropsClosedTabsForThatSession() {
        BrowserRecentlyClosedTabs stack = new BrowserRecentlyClosedTabs();
        stack.push(new BrowserClosedTab("session-a", "https://a.example/", "A", false, 0));
        stack.push(new BrowserClosedTab("session-b", "https://b.example/", "B", false, 0));
        stack.push(new BrowserClosedTab("session-a", "https://a2.example/", "A2", false, 1));

        stack.removeSession("session-a");

        Assert.assertEquals(1, stack.size());
        Assert.assertEquals("https://b.example/", stack.pop().getUrl());
    }
}
