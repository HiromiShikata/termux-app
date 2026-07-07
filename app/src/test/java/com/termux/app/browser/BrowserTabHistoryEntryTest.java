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

    @Test
    public void equalEntriesShareUrlTitleAndBody() {
        BrowserTabHistoryEntry first = new BrowserTabHistoryEntry(
            "https://example.com/page", "Title", "body");
        BrowserTabHistoryEntry second = new BrowserTabHistoryEntry(
            "https://example.com/page", "Title", "body");
        Assert.assertEquals(first, second);
        Assert.assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    public void entriesDifferingInAnyFieldAreNotEqual() {
        BrowserTabHistoryEntry base = new BrowserTabHistoryEntry(
            "https://example.com/page", "Title", "body");
        Assert.assertNotEquals(base,
            new BrowserTabHistoryEntry("https://example.com/other", "Title", "body"));
        Assert.assertNotEquals(base,
            new BrowserTabHistoryEntry("https://example.com/page", "Other", "body"));
        Assert.assertNotEquals(base,
            new BrowserTabHistoryEntry("https://example.com/page", "Title", "other"));
    }

    @Test
    public void closedAtMillisDefaultsToNull() {
        BrowserTabHistoryEntry entry = new BrowserTabHistoryEntry(
            "https://example.com/page", "Title", "body");
        Assert.assertNull(entry.getClosedAtMillis());
    }

    @Test
    public void withClosedAtMillisSetsCloseTimeAndKeepsOtherFields() {
        BrowserTabHistoryEntry entry = new BrowserTabHistoryEntry(
            "https://example.com/page", "Title", "body").withClosedAtMillis(1234L);
        Assert.assertEquals("https://example.com/page", entry.getUrl());
        Assert.assertEquals("Title", entry.getTitle());
        Assert.assertEquals("body", entry.getBodySnippet());
        Assert.assertEquals(Long.valueOf(1234L), entry.getClosedAtMillis());
    }

    @Test
    public void entriesDifferingOnlyInCloseTimeAreNotEqual() {
        BrowserTabHistoryEntry open = new BrowserTabHistoryEntry(
            "https://example.com/page", "Title", "body", null);
        BrowserTabHistoryEntry closed = new BrowserTabHistoryEntry(
            "https://example.com/page", "Title", "body", 1000L);
        Assert.assertNotEquals(open, closed);
    }

    @Test
    public void entriesWithSameCloseTimeAreEqual() {
        BrowserTabHistoryEntry first = new BrowserTabHistoryEntry(
            "https://example.com/page", "Title", "body", 1000L);
        BrowserTabHistoryEntry second = new BrowserTabHistoryEntry(
            "https://example.com/page", "Title", "body", 1000L);
        Assert.assertEquals(first, second);
        Assert.assertEquals(first.hashCode(), second.hashCode());
    }
}
