package com.termux.app.browser;

import org.json.JSONException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class BrowserPersistedTabsSerializerTest {

    private final BrowserPersistedTabsSerializer serializer = new BrowserPersistedTabsSerializer();

    @Test
    public void deserializeOfEmptyOrNullReturnsEmptyList() throws JSONException {
        Assert.assertTrue(serializer.deserialize("").isEmpty());
        Assert.assertTrue(serializer.deserialize(null).isEmpty());
    }

    @Test
    public void serializeOfEmptyListRoundTripsToEmptyList() throws JSONException {
        Assert.assertTrue(serializer.deserialize(serializer.serialize(new ArrayList<>())).isEmpty());
    }

    @Test
    public void roundTripPreservesTabUrlTitleDesktopModeAndOrder() throws JSONException {
        List<BrowserPersistedTab> tabs = Arrays.asList(
            new BrowserPersistedTab("https://a.example/", "Tab A", true),
            new BrowserPersistedTab("https://b.example/", "Tab B", false));
        List<BrowserPersistedSessionTabs> sessions = Arrays.asList(
            new BrowserPersistedSessionTabs("session-a", tabs, 1));

        List<BrowserPersistedSessionTabs> restored =
            serializer.deserialize(serializer.serialize(sessions));

        Assert.assertEquals(1, restored.size());
        BrowserPersistedSessionTabs restoredSession = restored.get(0);
        Assert.assertEquals("session-a", restoredSession.getSessionName());
        Assert.assertEquals(1, restoredSession.getActiveTabIndex());
        Assert.assertEquals(2, restoredSession.getTabs().size());
        Assert.assertEquals("https://a.example/", restoredSession.getTabs().get(0).getUrl());
        Assert.assertEquals("Tab A", restoredSession.getTabs().get(0).getTitle());
        Assert.assertTrue(restoredSession.getTabs().get(0).isDesktopMode());
        Assert.assertEquals("https://b.example/", restoredSession.getTabs().get(1).getUrl());
        Assert.assertFalse(restoredSession.getTabs().get(1).isDesktopMode());
    }

    @Test
    public void roundTripKeepsSessionsIndependentlyScoped() throws JSONException {
        List<BrowserPersistedSessionTabs> sessions = Arrays.asList(
            new BrowserPersistedSessionTabs("session-a",
                Arrays.asList(new BrowserPersistedTab("https://a.example/", "A", true)), 0),
            new BrowserPersistedSessionTabs("session-b",
                Arrays.asList(
                    new BrowserPersistedTab("https://b1.example/", "B1", true),
                    new BrowserPersistedTab("https://b2.example/", "B2", true)),
                0));

        List<BrowserPersistedSessionTabs> restored =
            serializer.deserialize(serializer.serialize(sessions));

        Assert.assertEquals(2, restored.size());
        Assert.assertEquals("session-a", restored.get(0).getSessionName());
        Assert.assertEquals(1, restored.get(0).getTabs().size());
        Assert.assertEquals("session-b", restored.get(1).getSessionName());
        Assert.assertEquals(2, restored.get(1).getTabs().size());
    }

    @Test
    public void sessionsWithNoTabsAreNotSerialized() throws JSONException {
        List<BrowserPersistedSessionTabs> sessions = Arrays.asList(
            new BrowserPersistedSessionTabs("empty-session", new ArrayList<>(), 0));

        Assert.assertTrue(serializer.deserialize(serializer.serialize(sessions)).isEmpty());
    }

    @Test
    public void activeTabIndexOutOfBoundsIsClampedOnConstruction() {
        BrowserPersistedSessionTabs session = new BrowserPersistedSessionTabs("session-a",
            Arrays.asList(new BrowserPersistedTab("https://a.example/", "A", true)), 5);

        Assert.assertEquals(0, session.getActiveTabIndex());
    }

    @Test
    public void roundTripPreservesUrlAndTitleWithSpecialCharacters() throws JSONException {
        List<BrowserPersistedSessionTabs> sessions = Arrays.asList(
            new BrowserPersistedSessionTabs("session-a",
                Arrays.asList(new BrowserPersistedTab(
                    "https://example.com/search?q=a+b&lang=en",
                    "Title with \"quotes\" and / slash", true)),
                0));

        List<BrowserPersistedSessionTabs> restored =
            serializer.deserialize(serializer.serialize(sessions));

        Assert.assertEquals("https://example.com/search?q=a+b&lang=en",
            restored.get(0).getTabs().get(0).getUrl());
        Assert.assertEquals("Title with \"quotes\" and / slash",
            restored.get(0).getTabs().get(0).getTitle());
    }

    @Test
    public void roundTripPreservesBookmarks() throws JSONException {
        List<BrowserBookmark> bookmarks = Arrays.asList(
            new BrowserBookmark("https://example.com/", "Example"),
            new BrowserBookmark("https://termux.dev/", "Termux"));
        List<BrowserPersistedSessionTabs> sessions = Arrays.asList(
            new BrowserPersistedSessionTabs("session-a",
                Arrays.asList(new BrowserPersistedTab("https://a.example/", "A", true)),
                0, bookmarks, new BrowserTabHistory(), null));

        List<BrowserPersistedSessionTabs> restored =
            serializer.deserialize(serializer.serialize(sessions));

        Assert.assertEquals(1, restored.size());
        List<BrowserBookmark> restoredBookmarks = restored.get(0).getBookmarks();
        Assert.assertEquals(2, restoredBookmarks.size());
        Assert.assertEquals("https://example.com/", restoredBookmarks.get(0).getUrl());
        Assert.assertEquals("Example", restoredBookmarks.get(0).getTitle());
        Assert.assertEquals("https://termux.dev/", restoredBookmarks.get(1).getUrl());
    }

    @Test
    public void roundTripPreservesHistory() throws JSONException {
        BrowserTabHistory history = new BrowserTabHistory()
            .recorded("https://example.com/first", "First")
            .recorded("https://example.com/second", "Second");
        List<BrowserPersistedSessionTabs> sessions = Arrays.asList(
            new BrowserPersistedSessionTabs("session-a",
                Arrays.asList(new BrowserPersistedTab("https://a.example/", "A", true)),
                0, new ArrayList<>(), history, null));

        List<BrowserPersistedSessionTabs> restored =
            serializer.deserialize(serializer.serialize(sessions));

        Assert.assertEquals(1, restored.size());
        BrowserTabHistory restoredHistory = restored.get(0).getHistory();
        Assert.assertEquals(2, restoredHistory.getEntries().size());
        Assert.assertEquals("https://example.com/second", restoredHistory.getEntries().get(0).getUrl());
        Assert.assertEquals("https://example.com/first", restoredHistory.getEntries().get(1).getUrl());
    }

    @Test
    public void roundTripPreservesDeletedAtMillis() throws JSONException {
        BrowserBookmark bookmark = new BrowserBookmark("https://example.com/", "Example");
        List<BrowserPersistedSessionTabs> sessions = Arrays.asList(
            new BrowserPersistedSessionTabs("session-a",
                new ArrayList<>(), 0,
                Collections.singletonList(bookmark), new BrowserTabHistory(), 1700000000000L));

        List<BrowserPersistedSessionTabs> restored =
            serializer.deserialize(serializer.serialize(sessions));

        Assert.assertEquals(1, restored.size());
        Assert.assertEquals(Long.valueOf(1700000000000L), restored.get(0).getDeletedAtMillis());
        Assert.assertTrue(restored.get(0).isDeleted());
    }

    @Test
    public void sessionsWithOnlyBookmarksButNoTabsAreSerialized() throws JSONException {
        List<BrowserBookmark> bookmarks = Collections.singletonList(
            new BrowserBookmark("https://example.com/", "Example"));
        List<BrowserPersistedSessionTabs> sessions = Arrays.asList(
            new BrowserPersistedSessionTabs("session-a",
                new ArrayList<>(), 0, bookmarks, new BrowserTabHistory(), null));

        List<BrowserPersistedSessionTabs> restored =
            serializer.deserialize(serializer.serialize(sessions));

        Assert.assertEquals(1, restored.size());
        Assert.assertEquals("session-a", restored.get(0).getSessionName());
        Assert.assertTrue(restored.get(0).getTabs().isEmpty());
        Assert.assertEquals(1, restored.get(0).getBookmarks().size());
    }

    @Test
    public void sessionsWithDeletedMarkerAreSerialized() throws JSONException {
        BrowserBookmark bookmark = new BrowserBookmark("https://saved.example/", "Saved");
        List<BrowserPersistedSessionTabs> sessions = Arrays.asList(
            new BrowserPersistedSessionTabs("deleted-session",
                new ArrayList<>(), 0,
                Collections.singletonList(bookmark), new BrowserTabHistory(), 9999999999999L));

        List<BrowserPersistedSessionTabs> restored =
            serializer.deserialize(serializer.serialize(sessions));

        Assert.assertEquals(1, restored.size());
        Assert.assertEquals("deleted-session", restored.get(0).getSessionName());
        Assert.assertEquals(Long.valueOf(9999999999999L), restored.get(0).getDeletedAtMillis());
    }

    @Test
    public void pruneStaleDeletedRemovesEntriesOlderThanTenDays() {
        long tenDaysMs = 10L * 24 * 60 * 60 * 1000;
        long currentTimeMillis = 2000000000000L;
        long staleDeletionTime = currentTimeMillis - tenDaysMs - 1;
        BrowserBookmark bookmark = new BrowserBookmark("https://example.com/", "Old");
        List<BrowserPersistedSessionTabs> sessions = Arrays.asList(
            new BrowserPersistedSessionTabs("stale-session",
                new ArrayList<>(), 0,
                Collections.singletonList(bookmark), new BrowserTabHistory(), staleDeletionTime));

        List<BrowserPersistedSessionTabs> pruned =
            BrowserPersistedTabsSerializer.pruneStaleDeleted(sessions, currentTimeMillis);

        Assert.assertTrue(pruned.isEmpty());
    }

    @Test
    public void pruneStaleDeletedKeepsEntriesWithinTenDays() {
        long tenDaysMs = 10L * 24 * 60 * 60 * 1000;
        long currentTimeMillis = 2000000000000L;
        long recentDeletionTime = currentTimeMillis - tenDaysMs + 1;
        BrowserBookmark bookmark = new BrowserBookmark("https://example.com/", "Recent");
        List<BrowserPersistedSessionTabs> sessions = Arrays.asList(
            new BrowserPersistedSessionTabs("recent-session",
                new ArrayList<>(), 0,
                Collections.singletonList(bookmark), new BrowserTabHistory(), recentDeletionTime));

        List<BrowserPersistedSessionTabs> pruned =
            BrowserPersistedTabsSerializer.pruneStaleDeleted(sessions, currentTimeMillis);

        Assert.assertEquals(1, pruned.size());
        Assert.assertEquals("recent-session", pruned.get(0).getSessionName());
    }

    @Test
    public void pruneStaleDeletedKeepsActiveSessionsWithNoDeletedMarker() {
        List<BrowserPersistedSessionTabs> sessions = Arrays.asList(
            new BrowserPersistedSessionTabs("active-session",
                Arrays.asList(new BrowserPersistedTab("https://a.example/", "A", true)), 0));

        List<BrowserPersistedSessionTabs> pruned =
            BrowserPersistedTabsSerializer.pruneStaleDeleted(sessions, Long.MAX_VALUE);

        Assert.assertEquals(1, pruned.size());
        Assert.assertEquals("active-session", pruned.get(0).getSessionName());
    }
}
