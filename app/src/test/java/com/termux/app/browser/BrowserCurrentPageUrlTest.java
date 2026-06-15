package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

public class BrowserCurrentPageUrlTest {

    @Test
    public void prefersDisplayedTabUrlOverLoadedUrl() {
        Assert.assertEquals(
            "https://github.com/org/repo/issues/123",
            BrowserCurrentPageUrl.fullUrl(
                "https://github.com/org/repo/issues/123",
                "https://github.com/org/repo"));
    }

    @Test
    public void fallsBackToLoadedUrlWhenTabUrlMissing() {
        Assert.assertEquals(
            "https://example.com/page",
            BrowserCurrentPageUrl.fullUrl(null, "https://example.com/page"));
    }

    @Test
    public void fallsBackToLoadedUrlWhenTabUrlBlank() {
        Assert.assertEquals(
            "https://example.com/page",
            BrowserCurrentPageUrl.fullUrl("   ", "https://example.com/page"));
    }

    @Test
    public void returnsNullWhenNoUrlAvailable() {
        Assert.assertNull(BrowserCurrentPageUrl.fullUrl(null, null));
    }

    @Test
    public void returnsNullWhenAllUrlsBlank() {
        Assert.assertNull(BrowserCurrentPageUrl.fullUrl("  ", "   "));
    }

    @Test
    public void trimsSurroundingWhitespaceFromSelectedUrl() {
        Assert.assertEquals(
            "https://example.com",
            BrowserCurrentPageUrl.fullUrl("  https://example.com  ", null));
    }

    @Test
    public void hasCopyableUrlReflectsAvailability() {
        Assert.assertTrue(BrowserCurrentPageUrl.hasCopyableUrl("https://example.com", null));
        Assert.assertFalse(BrowserCurrentPageUrl.hasCopyableUrl(null, null));
    }
}
