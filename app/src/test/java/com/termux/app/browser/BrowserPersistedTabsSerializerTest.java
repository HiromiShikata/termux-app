package com.termux.app.browser;

import org.json.JSONException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.Arrays;
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
}
