package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class BrowserTabHistoryTest {

    @Test
    public void recordedAddsEntry() {
        BrowserTabHistory history = new BrowserTabHistory()
            .recorded("https://example.com/page", "Example");

        List<BrowserTabHistoryEntry> entries = history.getEntries();
        Assert.assertEquals(1, entries.size());
        Assert.assertEquals("https://example.com/page", entries.get(0).getUrl());
        Assert.assertEquals("Example", entries.get(0).getTitle());
    }

    @Test
    public void recordedPlacesMostRecentFirst() {
        BrowserTabHistory history = new BrowserTabHistory()
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
    public void recordedMovesReaccessedEntryToFront() {
        BrowserTabHistory history = new BrowserTabHistory()
            .recorded("https://example.com/a", "A")
            .recorded("https://example.com/b", "B")
            .recorded("https://example.com/c", "C")
            .recorded("https://example.com/a", "A");

        List<BrowserTabHistoryEntry> entries = history.getEntries();
        Assert.assertEquals(3, entries.size());
        Assert.assertEquals("https://example.com/a", entries.get(0).getUrl());
        Assert.assertEquals("https://example.com/c", entries.get(1).getUrl());
        Assert.assertEquals("https://example.com/b", entries.get(2).getUrl());
    }

    @Test
    public void recordedDeduplicatesBySameUrlMovingToFront() {
        BrowserTabHistory history = new BrowserTabHistory()
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
        BrowserTabHistory history = new BrowserTabHistory()
            .recorded("https://example.net", "Example Net")
            .recorded("https://example.net/", "Example Net");

        List<BrowserTabHistoryEntry> entries = history.getEntries();
        Assert.assertEquals(1, entries.size());
        Assert.assertEquals("Example Net", entries.get(0).getTitle());
    }

    @Test
    public void recordedCollapsesTrailingSlashOnLongerPathIntoOneEntry() {
        BrowserTabHistory history = new BrowserTabHistory()
            .recorded("https://example.net/docs", "Docs")
            .recorded("https://example.net/docs/", "Docs");

        List<BrowserTabHistoryEntry> entries = history.getEntries();
        Assert.assertEquals(1, entries.size());
    }

    @Test
    public void recordedUpgradesUrlPlaceholderTitleToRealTitle() {
        BrowserTabHistory history = new BrowserTabHistory()
            .recorded("https://example.net/", "https://example.net/")
            .recorded("https://example.net/", "Real Title");

        List<BrowserTabHistoryEntry> entries = history.getEntries();
        Assert.assertEquals(1, entries.size());
        Assert.assertEquals("Real Title", entries.get(0).getTitle());
    }

    @Test
    public void recordedDoesNotDowngradeRealTitleToUrlPlaceholder() {
        BrowserTabHistory history = new BrowserTabHistory()
            .recorded("https://example.net/", "Real Title")
            .recorded("https://example.net/", "");

        List<BrowserTabHistoryEntry> entries = history.getEntries();
        Assert.assertEquals(1, entries.size());
        Assert.assertEquals("Real Title", entries.get(0).getTitle());
    }

    @Test
    public void recordedKeepsQueryStringDistinctFromBaseUrl() {
        BrowserTabHistory history = new BrowserTabHistory()
            .recorded("https://example.net/search", "Base")
            .recorded("https://example.net/search?q=1", "Query");

        List<BrowserTabHistoryEntry> entries = history.getEntries();
        Assert.assertEquals(2, entries.size());
    }

    @Test
    public void recordedRetainsEntryOfClosedTab() {
        BrowserTabHistory history = new BrowserTabHistory()
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
    public void recordedAccumulatesEntriesFromMultipleSessionsIntoOneStore() {
        BrowserTabHistory history = new BrowserTabHistory()
            .recorded("https://session-one.example.com/page", "Session one page")
            .recorded("https://session-two.example.com/page", "Session two page")
            .recorded("https://session-three.example.com/page", "Session three page");

        List<BrowserTabHistoryEntry> entries = history.getEntries();
        Assert.assertEquals(3, entries.size());
        Assert.assertEquals("https://session-three.example.com/page", entries.get(0).getUrl());
        Assert.assertEquals("https://session-two.example.com/page", entries.get(1).getUrl());
        Assert.assertEquals("https://session-one.example.com/page", entries.get(2).getUrl());
    }

    @Test
    public void recordedBoundsToMaxEntries() {
        BrowserTabHistory history = new BrowserTabHistory(new ArrayList<>(), 2)
            .recorded("https://example.com/1", "1")
            .recorded("https://example.com/2", "2")
            .recorded("https://example.com/3", "3");

        List<BrowserTabHistoryEntry> entries = history.getEntries();
        Assert.assertEquals(2, entries.size());
        Assert.assertEquals("https://example.com/3", entries.get(0).getUrl());
        Assert.assertEquals("https://example.com/2", entries.get(1).getUrl());
    }

    @Test
    public void defaultMaxEntriesIsGenerous() {
        Assert.assertTrue(BrowserTabHistory.DEFAULT_MAX_ENTRIES >= 500);
    }

    @Test
    public void recordedIgnoresEmptyUrl() {
        BrowserTabHistory history = new BrowserTabHistory()
            .recorded("", "No URL");

        Assert.assertTrue(history.isEmpty());
    }

    @Test
    public void recordedFallsBackToUrlWhenTitleEmpty() {
        BrowserTabHistory history = new BrowserTabHistory()
            .recorded("https://example.com/page", "");

        Assert.assertEquals("https://example.com/page", history.getEntries().get(0).getTitle());
    }

    @Test
    public void recordedReturnsNewInstanceLeavingOriginalUnchanged() {
        BrowserTabHistory original = new BrowserTabHistory()
            .recorded("https://example.com/a", "A");
        BrowserTabHistory updated = original.recorded("https://example.com/b", "B");

        Assert.assertEquals(1, original.getEntries().size());
        Assert.assertEquals(2, updated.getEntries().size());
    }

    @Test
    public void getEntriesReturnsUnmodifiableCopy() {
        BrowserTabHistory history = new BrowserTabHistory()
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

        BrowserTabHistory history = new BrowserTabHistory(seed, 2);

        List<BrowserTabHistoryEntry> entries = history.getEntries();
        Assert.assertEquals(2, entries.size());
        Assert.assertEquals("https://example.com/1", entries.get(0).getUrl());
        Assert.assertEquals("https://example.com/2", entries.get(1).getUrl());
    }

    @Test
    public void recordedRetainsBodySnippetWhenLaterRecordOmitsIt() {
        BrowserTabHistory history = new BrowserTabHistory()
            .recorded("https://example.com/page", "Example", "The quick brown fox")
            .recorded("https://example.com/page", "Example");

        Assert.assertEquals("The quick brown fox", history.getEntries().get(0).getBodySnippet());
    }

    @Test
    public void filteredWithEmptyQueryReturnsAllMostRecentFirst() {
        BrowserTabHistory history = new BrowserTabHistory()
            .recorded("https://example.com/first", "First")
            .recorded("https://example.com/second", "Second");

        List<BrowserTabHistoryEntry> filtered = history.filtered("");
        Assert.assertEquals(2, filtered.size());
        Assert.assertEquals("https://example.com/second", filtered.get(0).getUrl());
        Assert.assertEquals("https://example.com/first", filtered.get(1).getUrl());
    }

    @Test
    public void filteredMatchesTitleCaseInsensitively() {
        BrowserTabHistory history = new BrowserTabHistory()
            .recorded("https://example.com/a", "Weather Forecast")
            .recorded("https://example.com/b", "Recipe Book");

        List<BrowserTabHistoryEntry> filtered = history.filtered("WEATHER");
        Assert.assertEquals(1, filtered.size());
        Assert.assertEquals("https://example.com/a", filtered.get(0).getUrl());
    }

    @Test
    public void filteredMatchesUrlSubstring() {
        BrowserTabHistory history = new BrowserTabHistory()
            .recorded("https://github.com/HiromiShikata/termux-app", "Termux App")
            .recorded("https://example.com/other", "Other");

        List<BrowserTabHistoryEntry> filtered = history.filtered("termux");
        Assert.assertEquals(1, filtered.size());
        Assert.assertEquals("https://github.com/HiromiShikata/termux-app", filtered.get(0).getUrl());
    }

    @Test
    public void filteredMatchesBodySnippet() {
        BrowserTabHistory history = new BrowserTabHistory()
            .recorded("https://example.com/article", "An Article", "Cheese making at home")
            .recorded("https://example.com/other", "Other", "Unrelated content");

        List<BrowserTabHistoryEntry> filtered = history.filtered("cheese");
        Assert.assertEquals(1, filtered.size());
        Assert.assertEquals("https://example.com/article", filtered.get(0).getUrl());
    }

    @Test
    public void filteredRequiresAllWhitespaceSeparatedTokens() {
        BrowserTabHistory history = new BrowserTabHistory()
            .recorded("https://example.com/a", "Quarterly sales report")
            .recorded("https://example.com/b", "Quarterly marketing plan");

        List<BrowserTabHistoryEntry> filtered = history.filtered("quarterly sales");
        Assert.assertEquals(1, filtered.size());
        Assert.assertEquals("https://example.com/a", filtered.get(0).getUrl());
    }

    @Test
    public void filteredMatchesTokensAcrossTitleAndUrl() {
        BrowserTabHistory history = new BrowserTabHistory()
            .recorded("https://docs.example.com/guide", "Setup Guide")
            .recorded("https://blog.example.com/guide", "Setup Guide");

        List<BrowserTabHistoryEntry> filtered = history.filtered("docs setup");
        Assert.assertEquals(1, filtered.size());
        Assert.assertEquals("https://docs.example.com/guide", filtered.get(0).getUrl());
    }

    @Test
    public void filteredKeepsResultsInRecencyOrder() {
        BrowserTabHistory history = new BrowserTabHistory()
            .recorded("https://example.com/guide-old", "Guide old")
            .recorded("https://example.com/unrelated", "Unrelated")
            .recorded("https://example.com/guide-new", "Guide new");

        List<BrowserTabHistoryEntry> filtered = history.filtered("guide");
        Assert.assertEquals(2, filtered.size());
        Assert.assertEquals("https://example.com/guide-new", filtered.get(0).getUrl());
        Assert.assertEquals("https://example.com/guide-old", filtered.get(1).getUrl());
    }

    @Test
    public void filteredReturnsEmptyWhenNoTokenMatches() {
        BrowserTabHistory history = new BrowserTabHistory()
            .recorded("https://example.com/a", "Alpha");

        Assert.assertTrue(history.filtered("zzz").isEmpty());
    }

    @Test
    public void filteredIgnoresExtraWhitespaceInQuery() {
        BrowserTabHistory history = new BrowserTabHistory()
            .recorded("https://example.com/a", "Alpha Beta");

        List<BrowserTabHistoryEntry> filtered = history.filtered("   alpha    beta   ");
        Assert.assertEquals(1, filtered.size());
    }

    @Test
    public void filteredReturnsUnmodifiableList() {
        BrowserTabHistory history = new BrowserTabHistory()
            .recorded("https://example.com/a", "Alpha");

        try {
            history.filtered("").add(new BrowserTabHistoryEntry("https://example.com/x", "X"));
            Assert.fail("expected UnsupportedOperationException");
        } catch (UnsupportedOperationException expected) {
            // expected
        }
    }
}
