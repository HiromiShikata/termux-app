package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

public class BrowserTabHistoryEntryTest {

    @Test
    public void titleFallsBackToUrlWhenEmpty() {
        BrowserTabHistoryEntry entry = new BrowserTabHistoryEntry("https://example.com/page", "");
        Assert.assertEquals("https://example.com/page", entry.getTitle());
    }

    @Test
    public void bodySnippetDefaultsToEmpty() {
        BrowserTabHistoryEntry entry = new BrowserTabHistoryEntry("https://example.com/page", "Title");
        Assert.assertEquals("", entry.getBodySnippet());
    }

    @Test
    public void bodySnippetCollapsesWhitespace() {
        BrowserTabHistoryEntry entry = new BrowserTabHistoryEntry(
            "https://example.com/page", "Title", "  line one\n\n line   two\t");
        Assert.assertEquals("line one line two", entry.getBodySnippet());
    }

    @Test
    public void bodySnippetIsBoundedToMaxLength() {
        StringBuilder longBody = new StringBuilder();
        for (int i = 0; i < BrowserTabHistoryEntry.MAX_BODY_SNIPPET_LENGTH + 500; i++) {
            longBody.append('x');
        }
        BrowserTabHistoryEntry entry = new BrowserTabHistoryEntry(
            "https://example.com/page", "Title", longBody.toString());
        Assert.assertEquals(BrowserTabHistoryEntry.MAX_BODY_SNIPPET_LENGTH, entry.getBodySnippet().length());
    }
}
