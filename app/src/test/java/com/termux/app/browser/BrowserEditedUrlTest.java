package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class BrowserEditedUrlTest {

    @Test
    public void usesEnteredUrlAsSessionName() {
        Assert.assertEquals(
            "https://github.com/org/repo/issues/123",
            BrowserEditedUrl.sessionNameFor("https://github.com/org/repo/issues/123"));
    }

    @Test
    public void trimsSurroundingWhitespaceFromSessionName() {
        Assert.assertEquals(
            "https://example.com",
            BrowserEditedUrl.sessionNameFor("  https://example.com  "));
    }

    @Test
    public void returnsNullSessionNameForBlankInput() {
        Assert.assertNull(BrowserEditedUrl.sessionNameFor("   "));
    }

    @Test
    public void returnsNullSessionNameForNullInput() {
        Assert.assertNull(BrowserEditedUrl.sessionNameFor(null));
    }

    @Test
    public void trimmedOrNullKeepsEnteredUrl() {
        Assert.assertEquals(
            "https://example.com",
            BrowserEditedUrl.trimmedOrNull("https://example.com"));
    }

    @Test
    public void trimmedOrNullTrimsWhitespace() {
        Assert.assertEquals(
            "https://example.com",
            BrowserEditedUrl.trimmedOrNull("  https://example.com  "));
    }

    @Test
    public void trimmedOrNullReturnsNullForBlank() {
        Assert.assertNull(BrowserEditedUrl.trimmedOrNull("   "));
    }

    @Test
    public void trimmedOrNullReturnsNullForNull() {
        Assert.assertNull(BrowserEditedUrl.trimmedOrNull(null));
    }
}
