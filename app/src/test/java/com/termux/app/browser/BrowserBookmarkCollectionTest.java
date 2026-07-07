package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BrowserBookmarkCollectionTest {

    @Test
    public void addedAppendsNewBookmark() {
        BrowserBookmarkCollection collection = new BrowserBookmarkCollection(new ArrayList<>())
            .added(new BrowserBookmark("https://example.com/", "Example"));

        List<BrowserBookmark> bookmarks = collection.getBookmarks();
        Assert.assertEquals(1, bookmarks.size());
        Assert.assertEquals("https://example.com/", bookmarks.get(0).getUrl());
        Assert.assertEquals("Example", bookmarks.get(0).getTitle());
    }

    @Test
    public void addedDeduplicatesBySameUrl() {
        BrowserBookmarkCollection collection = new BrowserBookmarkCollection(new ArrayList<>())
            .added(new BrowserBookmark("https://example.com/", "Example"))
            .added(new BrowserBookmark("https://example.com/", "Example Renamed"));

        List<BrowserBookmark> bookmarks = collection.getBookmarks();
        Assert.assertEquals(1, bookmarks.size());
        Assert.assertEquals("Example", bookmarks.get(0).getTitle());
    }

    @Test
    public void addedKeepsDistinctUrls() {
        BrowserBookmarkCollection collection = new BrowserBookmarkCollection(new ArrayList<>())
            .added(new BrowserBookmark("https://example.com/", "Example"))
            .added(new BrowserBookmark("https://termux.dev/", "Termux"));

        Assert.assertEquals(2, collection.getBookmarks().size());
    }

    @Test
    public void removedDropsMatchingUrl() {
        BrowserBookmarkCollection collection = new BrowserBookmarkCollection(Arrays.asList(
            new BrowserBookmark("https://example.com/", "Example"),
            new BrowserBookmark("https://termux.dev/", "Termux")))
            .removed("https://example.com/");

        List<BrowserBookmark> bookmarks = collection.getBookmarks();
        Assert.assertEquals(1, bookmarks.size());
        Assert.assertEquals("https://termux.dev/", bookmarks.get(0).getUrl());
    }

    @Test
    public void removedOfMissingUrlLeavesCollectionUnchanged() {
        BrowserBookmarkCollection collection = new BrowserBookmarkCollection(Arrays.asList(
            new BrowserBookmark("https://example.com/", "Example")))
            .removed("https://termux.dev/");

        Assert.assertEquals(1, collection.getBookmarks().size());
    }

    @Test
    public void containsReportsPresenceByUrl() {
        BrowserBookmarkCollection collection = new BrowserBookmarkCollection(Arrays.asList(
            new BrowserBookmark("https://example.com/", "Example")));

        Assert.assertTrue(collection.contains("https://example.com/"));
        Assert.assertFalse(collection.contains("https://termux.dev/"));
    }

    @Test
    public void toggledAddsBookmarkWhenAbsent() {
        BrowserBookmarkCollection collection = new BrowserBookmarkCollection(new ArrayList<>())
            .toggled(new BrowserBookmark("https://example.com/", "Example"));

        Assert.assertTrue(collection.contains("https://example.com/"));
        Assert.assertEquals(1, collection.getBookmarks().size());
    }

    @Test
    public void toggledRemovesBookmarkWhenPresent() {
        BrowserBookmarkCollection collection = new BrowserBookmarkCollection(Arrays.asList(
            new BrowserBookmark("https://example.com/", "Example"),
            new BrowserBookmark("https://termux.dev/", "Termux")))
            .toggled(new BrowserBookmark("https://example.com/", "Example"));

        Assert.assertFalse(collection.contains("https://example.com/"));
        Assert.assertTrue(collection.contains("https://termux.dev/"));
        Assert.assertEquals(1, collection.getBookmarks().size());
    }

    @Test
    public void toggledTwiceRestoresOriginalState() {
        BrowserBookmark bookmark = new BrowserBookmark("https://example.com/", "Example");
        BrowserBookmarkCollection collection = new BrowserBookmarkCollection(new ArrayList<>())
            .toggled(bookmark)
            .toggled(bookmark);

        Assert.assertFalse(collection.contains("https://example.com/"));
        Assert.assertTrue(collection.getBookmarks().isEmpty());
    }

    @Test
    public void filteredWithEmptyQueryReturnsAllBookmarks() {
        List<BrowserBookmark> all = Arrays.asList(
            new BrowserBookmark("https://example.com/", "Example"),
            new BrowserBookmark("https://termux.dev/", "Termux"));

        List<BrowserBookmark> filtered = BrowserBookmarkCollection.filtered("", all);

        Assert.assertEquals(2, filtered.size());
    }

    @Test
    public void filteredWithBlankQueryReturnsAllBookmarks() {
        List<BrowserBookmark> all = Arrays.asList(
            new BrowserBookmark("https://example.com/", "Example"),
            new BrowserBookmark("https://termux.dev/", "Termux"));

        List<BrowserBookmark> filtered = BrowserBookmarkCollection.filtered("   ", all);

        Assert.assertEquals(2, filtered.size());
    }

    @Test
    public void filteredMatchesTitleCaseInsensitively() {
        List<BrowserBookmark> all = Arrays.asList(
            new BrowserBookmark("https://example.com/", "Example"),
            new BrowserBookmark("https://termux.dev/", "Termux"));

        List<BrowserBookmark> filtered = BrowserBookmarkCollection.filtered("term", all);

        Assert.assertEquals(1, filtered.size());
        Assert.assertEquals("https://termux.dev/", filtered.get(0).getUrl());
    }

    @Test
    public void filteredMatchesUrlCaseInsensitively() {
        List<BrowserBookmark> all = Arrays.asList(
            new BrowserBookmark("https://example.com/", "Example"),
            new BrowserBookmark("https://termux.dev/", "Termux"));

        List<BrowserBookmark> filtered = BrowserBookmarkCollection.filtered("EXAMPLE.COM", all);

        Assert.assertEquals(1, filtered.size());
        Assert.assertEquals("Example", filtered.get(0).getTitle());
    }

    @Test
    public void filteredMatchesEitherTitleOrUrl() {
        List<BrowserBookmark> all = Arrays.asList(
            new BrowserBookmark("https://search.example.com/", "Docs"),
            new BrowserBookmark("https://termux.dev/", "Search Termux"));

        List<BrowserBookmark> filtered = BrowserBookmarkCollection.filtered("search", all);

        Assert.assertEquals(2, filtered.size());
    }

    @Test
    public void filteredReturnsEmptyListWhenNoBookmarkMatches() {
        List<BrowserBookmark> all = Arrays.asList(
            new BrowserBookmark("https://example.com/", "Example"),
            new BrowserBookmark("https://termux.dev/", "Termux"));

        List<BrowserBookmark> filtered = BrowserBookmarkCollection.filtered("nonexistent", all);

        Assert.assertTrue(filtered.isEmpty());
    }

    @Test
    public void filteredPreservesInputOrder() {
        List<BrowserBookmark> all = Arrays.asList(
            new BrowserBookmark("https://a.example.com/", "Alpha docs"),
            new BrowserBookmark("https://b.example.com/", "Beta docs"),
            new BrowserBookmark("https://c.example.com/", "Gamma docs"));

        List<BrowserBookmark> filtered = BrowserBookmarkCollection.filtered("docs", all);

        Assert.assertEquals(3, filtered.size());
        Assert.assertEquals("Alpha docs", filtered.get(0).getTitle());
        Assert.assertEquals("Beta docs", filtered.get(1).getTitle());
        Assert.assertEquals("Gamma docs", filtered.get(2).getTitle());
    }
}
