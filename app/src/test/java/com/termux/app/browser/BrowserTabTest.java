package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

public class BrowserTabTest {

    @Test
    public void titleDefaultsToUrl() {
        BrowserTab tab = new BrowserTab("session", "https://example.com/");
        Assert.assertEquals("https://example.com/", tab.getTitle());
    }

    @Test
    public void emptyTitleFallsBackToUrl() {
        BrowserTab tab = new BrowserTab("session", "https://example.com/");
        tab.setUrl("https://example.com/page");
        tab.setTitle("");
        Assert.assertEquals("https://example.com/page", tab.getTitle());
    }

    @Test
    public void blankUrlUpdateIsIgnored() {
        BrowserTab tab = new BrowserTab("session", "https://example.com/");
        tab.setUrl("");
        tab.setUrl(null);
        Assert.assertEquals("https://example.com/", tab.getUrl());
    }

    @Test
    public void eachTabHasDistinctId() {
        BrowserTab first = new BrowserTab("session", "https://example.com/");
        BrowserTab second = new BrowserTab("session", "https://example.com/");
        Assert.assertNotEquals(first.getId(), second.getId());
    }

    @Test
    public void desktopModeDefaultsToTrue() {
        BrowserTab tab = new BrowserTab("session", "https://example.com/");
        Assert.assertTrue(tab.isDesktopMode());
    }

    @Test
    public void desktopModeCanBeToggled() {
        BrowserTab tab = new BrowserTab("session", "https://example.com/");
        tab.setDesktopMode(false);
        Assert.assertFalse(tab.isDesktopMode());
        tab.setDesktopMode(true);
        Assert.assertTrue(tab.isDesktopMode());
    }

    @Test
    public void loadingDefaultsToFalse() {
        BrowserTab tab = new BrowserTab("session", "https://example.com/");
        Assert.assertFalse(tab.isLoading());
    }

    @Test
    public void setLoadingToTrueMarksTabAsLoading() {
        BrowserTab tab = new BrowserTab("session", "https://example.com/");
        tab.setLoading(true);
        Assert.assertTrue(tab.isLoading());
    }

    @Test
    public void setLoadingToFalseMarksTabAsNotLoading() {
        BrowserTab tab = new BrowserTab("session", "https://example.com/");
        tab.setLoading(true);
        tab.setLoading(false);
        Assert.assertFalse(tab.isLoading());
    }
}
