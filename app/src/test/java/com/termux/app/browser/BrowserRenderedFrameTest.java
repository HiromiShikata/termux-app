package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

public class BrowserRenderedFrameTest {

    private static final String SESSION = "session-a";
    private static final String URL = "https://example.com/";

    @Test
    public void blankFrameHasNoTabOwnerOrUrl() {
        BrowserRenderedFrame frame = BrowserRenderedFrame.BLANK;
        Assert.assertTrue(frame.isBlank());
        Assert.assertNull(frame.getTab());
        Assert.assertNull(frame.getOwnerSessionHandle());
        Assert.assertNull(frame.getCommittedUrl());
    }

    @Test
    public void ownerSessionHandleIsDerivedFromTheTab() {
        BrowserTab tab = new BrowserTab(SESSION, URL);
        BrowserRenderedFrame frame = BrowserRenderedFrame.renderingTab(tab);
        Assert.assertEquals(SESSION, frame.getOwnerSessionHandle());
        Assert.assertSame(tab, frame.getTab());
        Assert.assertFalse(frame.isBlank());
    }

    @Test
    public void renderingTabStartsWithNullCommittedUrl() {
        BrowserTab tab = new BrowserTab(SESSION, URL);
        BrowserRenderedFrame frame = BrowserRenderedFrame.renderingTab(tab);
        Assert.assertNull(frame.getCommittedUrl());
    }

    @Test
    public void isDisplayingMatchesOnlyTheExactTabInstance() {
        BrowserTab tab = new BrowserTab(SESSION, URL);
        BrowserTab otherTab = new BrowserTab(SESSION, URL);
        BrowserRenderedFrame frame = BrowserRenderedFrame.renderingTab(tab);
        Assert.assertTrue(frame.isDisplaying(tab));
        Assert.assertFalse(frame.isDisplaying(otherTab));
        Assert.assertFalse(frame.isDisplaying(null));
        Assert.assertFalse(BrowserRenderedFrame.BLANK.isDisplaying(tab));
    }

    @Test
    public void withCommittedUrlPreservesTab() {
        BrowserTab tab = new BrowserTab(SESSION, URL);
        BrowserRenderedFrame frame = BrowserRenderedFrame.renderingTab(tab);
        BrowserRenderedFrame committed = frame.withCommittedUrl("https://example.com/page");
        Assert.assertEquals("https://example.com/page", committed.getCommittedUrl());
        Assert.assertSame(tab, committed.getTab());
        Assert.assertNull(frame.getCommittedUrl());
    }
}
