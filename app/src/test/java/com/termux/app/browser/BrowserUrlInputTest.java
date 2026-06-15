package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class BrowserUrlInputTest {

    @Test
    public void keepsAbsoluteUrlWithScheme() {
        Assert.assertEquals(
            "https://github.com/org/repo/issues/123",
            BrowserUrlInput.normalize("https://github.com/org/repo/issues/123"));
    }

    @Test
    public void prependsHttpsForBareDomain() {
        Assert.assertEquals(
            "https://example.com",
            BrowserUrlInput.normalize("example.com"));
    }

    @Test
    public void trimsSurroundingWhitespaceBeforeNormalizing() {
        Assert.assertEquals(
            "https://example.com",
            BrowserUrlInput.normalize("  example.com  "));
    }

    @Test
    public void searchesWhenInputHasNoDot() {
        Assert.assertEquals(
            "https://duckduckgo.com/?q=termux",
            BrowserUrlInput.normalize("termux"));
    }

    @Test
    public void searchesWhenInputContainsSpace() {
        Assert.assertEquals(
            "https://duckduckgo.com/?q=termux%20browser",
            BrowserUrlInput.normalize("termux browser"));
    }

    @Test
    public void returnsDefaultUrlForNullInput() {
        Assert.assertEquals(BrowserTab.DEFAULT_URL, BrowserUrlInput.normalize(null));
    }

    @Test
    public void returnsDefaultUrlForBlankInput() {
        Assert.assertEquals(BrowserTab.DEFAULT_URL, BrowserUrlInput.normalize("   "));
    }

    @Test
    public void keepsNonHttpSchemeUntouched() {
        Assert.assertEquals(
            "ftp://example.com/file",
            BrowserUrlInput.normalize("ftp://example.com/file"));
    }
}
