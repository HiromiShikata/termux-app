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
}
