package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class BrowserNewTabListTest {

    @Test
    public void emptyQueryReturnsAllBookmarksThenHistory() {
        List<BrowserBookmark> bookmarks = Arrays.asList(
            new BrowserBookmark("https://bookmark-a.example.com/", "Bookmark A"),
            new BrowserBookmark("https://bookmark-b.example.com/", "Bookmark B"));
        List<BrowserTabHistoryEntry> history = Arrays.asList(
            new BrowserTabHistoryEntry("https://history-a.example.com/", "History A"),
            new BrowserTabHistoryEntry("https://history-b.example.com/", "History B"));

        List<BrowserNewTabEntry> combined = BrowserNewTabList.combined("", bookmarks, history);

        Assert.assertEquals(4, combined.size());
        Assert.assertEquals("https://bookmark-a.example.com/", combined.get(0).getUrl());
        Assert.assertTrue(combined.get(0).isBookmark());
        Assert.assertEquals("https://bookmark-b.example.com/", combined.get(1).getUrl());
        Assert.assertTrue(combined.get(1).isBookmark());
        Assert.assertEquals("https://history-a.example.com/", combined.get(2).getUrl());
        Assert.assertFalse(combined.get(2).isBookmark());
        Assert.assertEquals("https://history-b.example.com/", combined.get(3).getUrl());
        Assert.assertFalse(combined.get(3).isBookmark());
    }

    @Test
    public void blankQueryReturnsAllEntries() {
        List<BrowserBookmark> bookmarks = Collections.singletonList(
            new BrowserBookmark("https://bookmark.example.com/", "Bookmark"));
        List<BrowserTabHistoryEntry> history = Collections.singletonList(
            new BrowserTabHistoryEntry("https://history.example.com/", "History"));

        List<BrowserNewTabEntry> combined = BrowserNewTabList.combined("   ", bookmarks, history);

        Assert.assertEquals(2, combined.size());
    }

    @Test
    public void bookmarksAreOrderedBeforeHistory() {
        List<BrowserBookmark> bookmarks = Collections.singletonList(
            new BrowserBookmark("https://zeta.example.com/", "Zeta bookmark"));
        List<BrowserTabHistoryEntry> history = Collections.singletonList(
            new BrowserTabHistoryEntry("https://alpha.example.com/", "Alpha history"));

        List<BrowserNewTabEntry> combined = BrowserNewTabList.combined("", bookmarks, history);

        Assert.assertEquals("https://zeta.example.com/", combined.get(0).getUrl());
        Assert.assertEquals("https://alpha.example.com/", combined.get(1).getUrl());
    }

    @Test
    public void duplicateUrlIsShownOncePreferringTheBookmark() {
        List<BrowserBookmark> bookmarks = Collections.singletonList(
            new BrowserBookmark("https://shared.example.com/", "Shared bookmark title"));
        List<BrowserTabHistoryEntry> history = Collections.singletonList(
            new BrowserTabHistoryEntry("https://shared.example.com/", "Shared history title"));

        List<BrowserNewTabEntry> combined = BrowserNewTabList.combined("", bookmarks, history);

        Assert.assertEquals(1, combined.size());
        Assert.assertEquals("https://shared.example.com/", combined.get(0).getUrl());
        Assert.assertEquals("Shared bookmark title", combined.get(0).getTitle());
        Assert.assertTrue(combined.get(0).isBookmark());
    }

    @Test
    public void duplicateUrlOwnedByBookmarkIsNotReintroducedByHistoryEvenWhenBookmarkFilteredOut() {
        List<BrowserBookmark> bookmarks = Collections.singletonList(
            new BrowserBookmark("https://shared.example.com/", "Bookmark label"));
        List<BrowserTabHistoryEntry> history = Collections.singletonList(
            new BrowserTabHistoryEntry("https://shared.example.com/", "History label"));

        List<BrowserNewTabEntry> combined = BrowserNewTabList.combined("History", bookmarks, history);

        Assert.assertTrue(combined.isEmpty());
    }

    @Test
    public void queryMatchesBookmarkTitleCaseInsensitively() {
        List<BrowserBookmark> bookmarks = Arrays.asList(
            new BrowserBookmark("https://example.com/", "Example"),
            new BrowserBookmark("https://termux.dev/", "Termux"));

        List<BrowserNewTabEntry> combined =
            BrowserNewTabList.combined("TERM", bookmarks, new ArrayList<>());

        Assert.assertEquals(1, combined.size());
        Assert.assertEquals("https://termux.dev/", combined.get(0).getUrl());
    }

    @Test
    public void queryMatchesBookmarkUrlCaseInsensitively() {
        List<BrowserBookmark> bookmarks = Arrays.asList(
            new BrowserBookmark("https://example.com/", "Docs"),
            new BrowserBookmark("https://termux.dev/", "Termux"));

        List<BrowserNewTabEntry> combined =
            BrowserNewTabList.combined("EXAMPLE.COM", bookmarks, new ArrayList<>());

        Assert.assertEquals(1, combined.size());
        Assert.assertEquals("Docs", combined.get(0).getTitle());
    }

    @Test
    public void queryMatchesHistoryTitleAndUrl() {
        List<BrowserTabHistoryEntry> history = Arrays.asList(
            new BrowserTabHistoryEntry("https://news.example.com/", "Morning news"),
            new BrowserTabHistoryEntry("https://weather.example.com/", "Forecast"));

        List<BrowserNewTabEntry> combinedByTitle =
            BrowserNewTabList.combined("news", new ArrayList<>(), history);
        Assert.assertEquals(1, combinedByTitle.size());
        Assert.assertEquals("https://news.example.com/", combinedByTitle.get(0).getUrl());
        Assert.assertFalse(combinedByTitle.get(0).isBookmark());

        List<BrowserNewTabEntry> combinedByUrl =
            BrowserNewTabList.combined("weather.example", new ArrayList<>(), history);
        Assert.assertEquals(1, combinedByUrl.size());
        Assert.assertEquals("Forecast", combinedByUrl.get(0).getTitle());
    }

    @Test
    public void queryFiltersBookmarksAndHistoryTogether() {
        List<BrowserBookmark> bookmarks = Arrays.asList(
            new BrowserBookmark("https://docs.bookmark.example.com/", "Bookmark docs"),
            new BrowserBookmark("https://other.example.com/", "Other bookmark"));
        List<BrowserTabHistoryEntry> history = Arrays.asList(
            new BrowserTabHistoryEntry("https://docs.history.example.com/", "History docs"),
            new BrowserTabHistoryEntry("https://misc.example.com/", "Misc history"));

        List<BrowserNewTabEntry> combined =
            BrowserNewTabList.combined("docs", bookmarks, history);

        Assert.assertEquals(2, combined.size());
        Assert.assertEquals("https://docs.bookmark.example.com/", combined.get(0).getUrl());
        Assert.assertTrue(combined.get(0).isBookmark());
        Assert.assertEquals("https://docs.history.example.com/", combined.get(1).getUrl());
        Assert.assertFalse(combined.get(1).isBookmark());
    }

    @Test
    public void noMatchReturnsEmptyList() {
        List<BrowserBookmark> bookmarks = Collections.singletonList(
            new BrowserBookmark("https://example.com/", "Example"));
        List<BrowserTabHistoryEntry> history = Collections.singletonList(
            new BrowserTabHistoryEntry("https://termux.dev/", "Termux"));

        List<BrowserNewTabEntry> combined =
            BrowserNewTabList.combined("nonexistent", bookmarks, history);

        Assert.assertTrue(combined.isEmpty());
    }

    @Test
    public void duplicateBookmarkUrlsAreCollapsedPreservingFirst() {
        List<BrowserBookmark> bookmarks = Arrays.asList(
            new BrowserBookmark("https://dup.example.com/", "First"),
            new BrowserBookmark("https://dup.example.com/", "Second"));

        List<BrowserNewTabEntry> combined =
            BrowserNewTabList.combined("", bookmarks, new ArrayList<>());

        Assert.assertEquals(1, combined.size());
        Assert.assertEquals("First", combined.get(0).getTitle());
    }
}
