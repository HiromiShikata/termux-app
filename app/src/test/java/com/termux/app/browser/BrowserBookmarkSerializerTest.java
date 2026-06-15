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
public class BrowserBookmarkSerializerTest {

    private final BrowserBookmarkSerializer serializer = new BrowserBookmarkSerializer();

    @Test
    public void deserializeOfEmptyStringReturnsEmptyList() throws JSONException {
        Assert.assertTrue(serializer.deserialize("").isEmpty());
        Assert.assertTrue(serializer.deserialize(null).isEmpty());
    }

    @Test
    public void serializeOfEmptyListRoundTripsToEmptyList() throws JSONException {
        String serialized = serializer.serialize(new ArrayList<>());
        Assert.assertTrue(serializer.deserialize(serialized).isEmpty());
    }

    @Test
    public void roundTripPreservesBookmark() throws JSONException {
        BrowserBookmark bookmark = new BrowserBookmark("https://example.com/", "Example Domain");

        List<BrowserBookmark> restored = serializer.deserialize(
            serializer.serialize(Arrays.asList(bookmark)));

        Assert.assertEquals(1, restored.size());
        Assert.assertEquals("https://example.com/", restored.get(0).getUrl());
        Assert.assertEquals("Example Domain", restored.get(0).getTitle());
    }

    @Test
    public void roundTripPreservesOrderOfMultipleBookmarks() throws JSONException {
        List<BrowserBookmark> bookmarks = Arrays.asList(
            new BrowserBookmark("https://example.com/", "Example"),
            new BrowserBookmark("https://termux.dev/", "Termux"),
            new BrowserBookmark("https://github.com/", "GitHub"));

        List<BrowserBookmark> restored = serializer.deserialize(serializer.serialize(bookmarks));

        Assert.assertEquals(3, restored.size());
        Assert.assertEquals("https://example.com/", restored.get(0).getUrl());
        Assert.assertEquals("Termux", restored.get(1).getTitle());
        Assert.assertEquals("https://github.com/", restored.get(2).getUrl());
    }

    @Test
    public void roundTripPreservesUrlAndTitleWithSpecialCharacters() throws JSONException {
        BrowserBookmark bookmark = new BrowserBookmark(
            "https://example.com/search?q=a+b&lang=en", "Title with \"quotes\" and / slash");

        List<BrowserBookmark> restored = serializer.deserialize(
            serializer.serialize(Arrays.asList(bookmark)));

        Assert.assertEquals(1, restored.size());
        Assert.assertEquals("https://example.com/search?q=a+b&lang=en", restored.get(0).getUrl());
        Assert.assertEquals("Title with \"quotes\" and / slash", restored.get(0).getTitle());
    }

    @Test(expected = JSONException.class)
    public void deserializeRejectsMalformedJson() throws JSONException {
        serializer.deserialize("not-json");
    }
}
