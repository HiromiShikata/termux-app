package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class BrowserSessionTabHistoryTest {

    @Test
    public void recordedAddsEntry() {
        BrowserSessionTabHistory history = new BrowserSessionTabHistory()
            .recorded("https://example.com/page", "Example");

        List<BrowserTabHistoryEntry> entries = history.getEntries();
        Assert.assertEquals(1, entries.size());
        Assert.assertEquals("https://example.com/page", entries.get(0).getUrl());
        Assert.assertEquals("Example", entries.get(0).getTitle());
    }

    @Test
    public void recordedPlacesMostRecentFirst() {
        BrowserSessionTabHistory history = new BrowserSessionTabHistory()
            .recorded("https://example.com/first", "First")
            .recorded("https://example.com/second", "Second")
            .recorded("https://example.com/third", "Third");

        List<BrowserTabHistoryEntry> entries = history.getEntries();
        Assert.assertEquals(3, entries.size());
        Assert.assertEquals("https://example.com/third", entries.get(0).getUrl());
        Assert.assertEquals("https://example.com/second", entries.get(1).getUrl());
        Assert.assertEquals("https://example.com/first", entries.get(2).getUrl());
    }

    @Test
    public void recordedDeduplicatesBySameUrlMovingToFront() {
        BrowserSessionTabHistory history = new BrowserSessionTabHistory()
            .recorded("https://example.com/a", "A")
            .recorded("https://example.com/b", "B")
            .recorded("https://example.com/a", "A again");

        List<BrowserTabHistoryEntry> entries = history.getEntries();
        Assert.assertEquals(2, entries.size());
        Assert.assertEquals("https://example.com/a", entries.get(0).getUrl());
        Assert.assertEquals("A again", entries.get(0).getTitle());
        Assert.assertEquals("https://example.com/b", entries.get(1).getUrl());
    }

    @Test
    public void recordedCollapsesTrailingSlashOnlyDifferenceIntoOneEntry() {
        BrowserSessionTabHistory history = new BrowserSessionTabHistory()
            .recorded("https://example.net", "Example Net")
            .recorded("https://example.net/", "Example Net");

        List<BrowserTabHistoryEntry> entries = history.getEntries();
        Assert.assertEquals(1, entries.size());
        Assert.assertEquals("Example Net", entries.get(0).getTitle());
    }

    @Test
    public void recordedCollapsesTrailingSlashOnLongerPathIntoOneEntry() {
        BrowserSessionTabHistory history = new BrowserSessionTabHistory()
            .recorded("https://example.net/docs", "Docs")
            .recorded("https://example.net/docs/", "Docs");

        List<BrowserTabHistoryEntry> entries = history.getEntries();
        Assert.assertEquals(1, entries.size());
    }

    @Test
    public void recordedUpgradesUrlPlaceholderTitleToRealTitle() {
        BrowserSessionTabHistory history = new BrowserSessionTabHistory()
            .recorded("https://example.net/", "https://example.net/")
            .recorded("https://example.net/", "Real Title");

        List<BrowserTabHistoryEntry> entries = history.getEntries();
        Assert.assertEquals(1, entries.size());
        Assert.assertEquals("Real Title", entries.get(0).getTitle());
    }

    @Test
    public void recordedDoesNotDowngradeRealTitleToUrlPlaceholder() {
        BrowserSessionTabHistory history = new BrowserSessionTabHistory()
            .recorded("https://example.net/", "Real Title")
            .recorded("https://example.net/", "");

        List<BrowserTabHistoryEntry> entries = history.getEntries();
        Assert.assertEquals(1, entries.size());
        Assert.assertEquals("Real Title", entries.get(0).getTitle());
    }

    @Test
    public void recordedKeepsQueryStringDistinctFromBaseUrl() {
        BrowserSessionTabHistory history = new BrowserSessionTabHistory()
            .recorded("https://example.net/search", "Base")
            .recorded("https://example.net/search?q=1", "Query");

        List<BrowserTabHistoryEntry> entries = history.getEntries();
        Assert.assertEquals(2, entries.size());
    }

    @Test
    public void recordedRetainsEntryOfClosedTab() {
        BrowserSessionTabHistory history = new BrowserSessionTabHistory()
            .recorded("https://example.com/closed", "Closed tab")
            .recorded("https://example.com/open", "Open tab");

        List<BrowserTabHistoryEntry> entries = history.getEntries();
        Assert.assertEquals(2, entries.size());
        boolean containsClosed = false;
        for (BrowserTabHistoryEntry entry : entries) {
            if (entry.getUrl().equals("https://example.com/closed")) containsClosed = true;
        }
        Assert.assertTrue(containsClosed);
    }

    @Test
    public void recordedBoundsToMaxEntries() {
        BrowserSessionTabHistory history = new BrowserSessionTabHistory(new ArrayList<>(), 2)
            .recorded("https://example.com/1", "1")
            .recorded("https://example.com/2", "2")
            .recorded("https://example.com/3", "3");

        List<BrowserTabHistoryEntry> entries = history.getEntries();
        Assert.assertEquals(2, entries.size());
        Assert.assertEquals("https://example.com/3", entries.get(0).getUrl());
        Assert.assertEquals("https://example.com/2", entries.get(1).getUrl());
    }

    @Test
    public void recordedIgnoresEmptyUrl() {
        BrowserSessionTabHistory history = new BrowserSessionTabHistory()
            .recorded("", "No URL");

        Assert.assertTrue(history.isEmpty());
    }

    @Test
    public void recordedFallsBackToUrlWhenTitleEmpty() {
        BrowserSessionTabHistory history = new BrowserSessionTabHistory()
            .recorded("https://example.com/page", "");

        Assert.assertEquals("https://example.com/page", history.getEntries().get(0).getTitle());
    }

    @Test
    public void recordedReturnsNewInstanceLeavingOriginalUnchanged() {
        BrowserSessionTabHistory original = new BrowserSessionTabHistory()
            .recorded("https://example.com/a", "A");
        BrowserSessionTabHistory updated = original.recorded("https://example.com/b", "B");

        Assert.assertEquals(1, original.getEntries().size());
        Assert.assertEquals(2, updated.getEntries().size());
    }

    @Test
    public void getEntriesReturnsUnmodifiableCopy() {
        BrowserSessionTabHistory history = new BrowserSessionTabHistory()
            .recorded("https://example.com/page", "Example");

        try {
            history.getEntries().add(new BrowserTabHistoryEntry("https://example.com/x", "X"));
            Assert.fail("expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
            // expected
        }
    }

    @Test
    public void constructorBoundsProvidedEntries() {
        List<BrowserTabHistoryEntry> seed = new ArrayList<>();
        seed.add(new BrowserTabHistoryEntry("https://example.com/1", "1"));
        seed.add(new BrowserTabHistoryEntry("https://example.com/2", "2"));
        seed.add(new BrowserTabHistoryEntry("https://example.com/3", "3"));

        BrowserSessionTabHistory history = new BrowserSessionTabHistory(seed, 2);

        List<BrowserTabHistoryEntry> entries = history.getEntries();
        Assert.assertEquals(2, entries.size());
        Assert.assertEquals("https://example.com/1", entries.get(0).getUrl());
        Assert.assertEquals("https://example.com/2", entries.get(1).getUrl());
    }
}
