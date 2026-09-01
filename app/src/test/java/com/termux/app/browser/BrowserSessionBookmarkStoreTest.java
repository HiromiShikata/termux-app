package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BrowserSessionBookmarkStoreTest {

    private static final BrowserBookmark BOOKMARK_A =
        new BrowserBookmark("https://a.example.com", "Page A");

    private static final BrowserBookmark BOOKMARK_B =
        new BrowserBookmark("https://b.example.com", "Page B");

    @Test
    public void loadReturnsEmptyCollectionForUnknownSession() {
        BrowserSessionBookmarkStore store =
            new BrowserSessionBookmarkStore(new LinkedHashMap<>());

        Assert.assertEquals(0, store.load("unknown").getBookmarks().size());
    }

    @Test
    public void loadReturnsEmptyCollectionWhenSessionNameIsNull() {
        BrowserSessionBookmarkStore store =
            new BrowserSessionBookmarkStore(new LinkedHashMap<>());

        Assert.assertEquals(0, store.load(null).getBookmarks().size());
    }

    @Test
    public void bookmarksSavedForSessionADoNotAppearWhenLoadingForSessionB() {
        Map<String, BrowserPersistedSessionTabs> sessions = new LinkedHashMap<>();
        BrowserSessionBookmarkStore store = new BrowserSessionBookmarkStore(sessions);

        store.save("session-a", Arrays.asList(BOOKMARK_A));

        Assert.assertEquals(1, store.load("session-a").getBookmarks().size());
        Assert.assertEquals(0, store.load("session-b").getBookmarks().size());
    }

    @Test
    public void saveWithNullSessionNameIsANoOp() {
        Map<String, BrowserPersistedSessionTabs> sessions = new LinkedHashMap<>();
        BrowserSessionBookmarkStore store = new BrowserSessionBookmarkStore(sessions);

        store.save(null, Arrays.asList(BOOKMARK_A));

        Assert.assertTrue(sessions.isEmpty());
    }

    @Test
    public void savePreservesExistingSessionTabsWhenUpdatingBookmarks() {
        Map<String, BrowserPersistedSessionTabs> sessions = new LinkedHashMap<>();
        List<BrowserPersistedTab> existingTabs = Arrays.asList(
            new BrowserPersistedTab("https://tab.example.com", "Tab", false));
        sessions.put("session-a", new BrowserPersistedSessionTabs(
            "session-a", existingTabs, 0, new ArrayList<>(), new BrowserTabHistory(), null));
        BrowserSessionBookmarkStore store = new BrowserSessionBookmarkStore(sessions);

        store.save("session-a", Arrays.asList(BOOKMARK_A));

        BrowserPersistedSessionTabs updated = sessions.get("session-a");
        Assert.assertEquals(1, updated.getTabs().size());
        Assert.assertEquals("https://tab.example.com", updated.getTabs().get(0).getUrl());
        Assert.assertEquals(1, updated.getBookmarks().size());
        Assert.assertEquals("https://a.example.com", updated.getBookmarks().get(0).getUrl());
    }

    @Test
    public void saveCreatesNewSessionEntryForPreviouslyUnknownSession() {
        Map<String, BrowserPersistedSessionTabs> sessions = new LinkedHashMap<>();
        BrowserSessionBookmarkStore store = new BrowserSessionBookmarkStore(sessions);

        store.save("new-session", Arrays.asList(BOOKMARK_B));

        Assert.assertTrue(sessions.containsKey("new-session"));
        Assert.assertEquals(1, sessions.get("new-session").getBookmarks().size());
        Assert.assertEquals("https://b.example.com",
            sessions.get("new-session").getBookmarks().get(0).getUrl());
    }

    @Test
    public void saveReplacesExistingBookmarksWithTheNewList() {
        Map<String, BrowserPersistedSessionTabs> sessions = new LinkedHashMap<>();
        BrowserSessionBookmarkStore store = new BrowserSessionBookmarkStore(sessions);
        store.save("session-a", Arrays.asList(BOOKMARK_A));

        store.save("session-a", Arrays.asList(BOOKMARK_B));

        List<BrowserBookmark> result = store.load("session-a").getBookmarks();
        Assert.assertEquals(1, result.size());
        Assert.assertEquals("https://b.example.com", result.get(0).getUrl());
    }
}
