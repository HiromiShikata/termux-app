package com.termux.app.browser;

import org.json.JSONException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class BrowserTabHistorySerializerTest {

    private final BrowserTabHistorySerializer mSerializer = new BrowserTabHistorySerializer();

    @Test
    public void deserializeNullReturnsEmptyHistory() throws JSONException {
        BrowserTabHistory history = mSerializer.deserialize(null, BrowserTabHistory.DEFAULT_MAX_ENTRIES);
        Assert.assertTrue(history.isEmpty());
    }

    @Test
    public void deserializeEmptyStringReturnsEmptyHistory() throws JSONException {
        BrowserTabHistory history = mSerializer.deserialize("", BrowserTabHistory.DEFAULT_MAX_ENTRIES);
        Assert.assertTrue(history.isEmpty());
    }

    @Test
    public void roundTripPreservesEntriesAndOrder() throws JSONException {
        BrowserTabHistory original = new BrowserTabHistory()
            .recorded("https://example.com/first", "First")
            .recorded("https://example.com/second", "Second");

        String serialized = mSerializer.serialize(original);
        BrowserTabHistory restored = mSerializer.deserialize(serialized, BrowserTabHistory.DEFAULT_MAX_ENTRIES);

        List<BrowserTabHistoryEntry> entries = restored.getEntries();
        Assert.assertEquals(2, entries.size());
        Assert.assertEquals("https://example.com/second", entries.get(0).getUrl());
        Assert.assertEquals("Second", entries.get(0).getTitle());
        Assert.assertEquals("https://example.com/first", entries.get(1).getUrl());
        Assert.assertEquals("First", entries.get(1).getTitle());
    }

    @Test
    public void roundTripPreservesBodySnippet() throws JSONException {
        BrowserTabHistory original = new BrowserTabHistory()
            .recorded("https://example.com/article", "Article", "The quick brown fox");

        String serialized = mSerializer.serialize(original);
        BrowserTabHistory restored = mSerializer.deserialize(serialized, BrowserTabHistory.DEFAULT_MAX_ENTRIES);

        Assert.assertEquals("The quick brown fox", restored.getEntries().get(0).getBodySnippet());
    }

    @Test
    public void deserializeSkipsEntriesWithEmptyUrl() throws JSONException {
        String serialized = "[{\"url\":\"\",\"title\":\"No URL\"},"
            + "{\"url\":\"https://example.com/keep\",\"title\":\"Keep\"}]";

        BrowserTabHistory restored = mSerializer.deserialize(serialized, BrowserTabHistory.DEFAULT_MAX_ENTRIES);

        List<BrowserTabHistoryEntry> entries = restored.getEntries();
        Assert.assertEquals(1, entries.size());
        Assert.assertEquals("https://example.com/keep", entries.get(0).getUrl());
    }

    @Test
    public void deserializeDefaultsTitleToUrlWhenMissing() throws JSONException {
        String serialized = "[{\"url\":\"https://example.com/notitle\"}]";

        BrowserTabHistory restored = mSerializer.deserialize(serialized, BrowserTabHistory.DEFAULT_MAX_ENTRIES);

        Assert.assertEquals("https://example.com/notitle", restored.getEntries().get(0).getTitle());
    }

    @Test
    public void deserializeAppliesMaxEntriesBound() throws JSONException {
        String serialized = "[{\"url\":\"https://example.com/1\",\"title\":\"1\"},"
            + "{\"url\":\"https://example.com/2\",\"title\":\"2\"},"
            + "{\"url\":\"https://example.com/3\",\"title\":\"3\"}]";

        BrowserTabHistory restored = mSerializer.deserialize(serialized, 2);

        List<BrowserTabHistoryEntry> entries = restored.getEntries();
        Assert.assertEquals(2, entries.size());
        Assert.assertEquals("https://example.com/1", entries.get(0).getUrl());
        Assert.assertEquals("https://example.com/2", entries.get(1).getUrl());
    }

    @Test
    public void serializeEmptyHistoryProducesEmptyJsonArray() throws JSONException {
        Assert.assertEquals("[]", mSerializer.serialize(new BrowserTabHistory()));
    }
}
