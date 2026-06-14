package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

public class BrowserPageHeaderTextTest {

    @Test
    public void combinesTitleAndUrlOnASingleLineWithSeparator() {
        Assert.assertEquals(
            "Example Domain — https://example.com/",
            BrowserPageHeaderText.format("Example Domain", "https://example.com/"));
    }

    @Test
    public void usesUrlAloneWhenTitleIsMissing() {
        Assert.assertEquals(
            "https://example.com/",
            BrowserPageHeaderText.format(null, "https://example.com/"));
    }

    @Test
    public void usesUrlAloneWhenTitleIsBlank() {
        Assert.assertEquals(
            "https://example.com/",
            BrowserPageHeaderText.format("   ", "https://example.com/"));
    }

    @Test
    public void usesTitleAloneWhenUrlIsMissing() {
        Assert.assertEquals(
            "Loading",
            BrowserPageHeaderText.format("Loading", null));
    }

    @Test
    public void returnsEmptyWhenNeitherTitleNorUrlIsPresent() {
        Assert.assertEquals("", BrowserPageHeaderText.format(null, null));
        Assert.assertEquals("", BrowserPageHeaderText.format("", "   "));
    }

    @Test
    public void doesNotRepeatUrlWhenTitleEqualsUrl() {
        Assert.assertEquals(
            "https://example.com/",
            BrowserPageHeaderText.format("https://example.com/", "https://example.com/"));
    }

    @Test
    public void trimsSurroundingWhitespaceFromTitleAndUrl() {
        Assert.assertEquals(
            "Example — https://example.com/",
            BrowserPageHeaderText.format("  Example  ", "  https://example.com/  "));
    }
}
