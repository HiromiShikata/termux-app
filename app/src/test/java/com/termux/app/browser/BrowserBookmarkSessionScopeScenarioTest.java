package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

public class BrowserBookmarkSessionScopeScenarioTest {

    @Test
    public void bookmarksSavedInSessionADoNotAppearInSessionB() {
        BrowserPersistedSessionTabs sessionA = new BrowserPersistedSessionTabs(
            "session-a", new ArrayList<>(), 0,
            Arrays.asList(new BrowserBookmark("https://example.com", "Example")),
            new BrowserTabHistory(), null);
        BrowserPersistedSessionTabs sessionB = new BrowserPersistedSessionTabs(
            "session-b", new ArrayList<>(), 0,
            new ArrayList<>(), new BrowserTabHistory(), null);

        Assert.assertEquals(1, sessionA.getBookmarks().size());
        Assert.assertEquals(0, sessionB.getBookmarks().size());
    }

    @Test
    public void sessionClosurePreservesBookmarksUnderDeletedAtMillisMarker() {
        BrowserPersistedSessionTabs session = new BrowserPersistedSessionTabs(
            "session-a", new ArrayList<>(), 0,
            Arrays.asList(new BrowserBookmark("https://example.com", "Example")),
            new BrowserTabHistory(), null);

        BrowserPersistedSessionTabs deleted = session.withDeletedAtMillis(1000L);

        Assert.assertTrue(deleted.isDeleted());
        Assert.assertEquals(1, deleted.getBookmarks().size());
    }

    @Test
    public void sessionReconnectClearsDeletedMarkerWhilePreservingBookmarks() {
        BrowserPersistedSessionTabs session = new BrowserPersistedSessionTabs(
            "session-a", new ArrayList<>(), 0,
            Arrays.asList(new BrowserBookmark("https://example.com", "Example")),
            new BrowserTabHistory(), null);

        BrowserPersistedSessionTabs restored =
            session.withDeletedAtMillis(1000L).withoutDeletedMarker();

        Assert.assertFalse(restored.isDeleted());
        Assert.assertEquals(1, restored.getBookmarks().size());
    }

    @Test
    public void pruneStaleDeletedRemovesSessionsOlderThan10DaysButKeepsRecentOnes() {
        long tenDaysMs = 10L * 24 * 60 * 60 * 1000;
        long now = 1_000_000_000_000L;

        BrowserPersistedSessionTabs stale = new BrowserPersistedSessionTabs(
            "session-a", new ArrayList<>(), 0,
            Arrays.asList(new BrowserBookmark("https://stale.example.com", "Stale")),
            new BrowserTabHistory(), null)
            .withDeletedAtMillis(now - tenDaysMs - 1);

        BrowserPersistedSessionTabs fresh = new BrowserPersistedSessionTabs(
            "session-b", new ArrayList<>(), 0,
            Arrays.asList(new BrowserBookmark("https://fresh.example.com", "Fresh")),
            new BrowserTabHistory(), null)
            .withDeletedAtMillis(now - tenDaysMs + 1);

        List<BrowserPersistedSessionTabs> pruned =
            BrowserPersistedTabsSerializer.pruneStaleDeleted(Arrays.asList(stale, fresh), now);

        Assert.assertEquals(1, pruned.size());
        Assert.assertEquals("session-b", pruned.get(0).getSessionName());
    }
}
