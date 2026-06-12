package com.termux.app.sessiondefinition;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
public class SessionDefinitionLoaderTest {

    private static final class RecordingFetcher implements SessionDefinitionDocumentFetcher {
        private final Map<String, String> documents = new HashMap<>();
        private final List<String> fetchedUrls = new ArrayList<>();

        void register(String url, String document) {
            documents.put(url, document);
        }

        @Override
        public String fetch(String url) throws IOException {
            fetchedUrls.add(url);
            String document = documents.get(url);
            if (document == null) {
                throw new IOException("No document registered for " + url);
            }
            return document;
        }
    }

    @Test
    public void loadProducesEntriesInGroupThenEntryOrder() throws Exception {
        RecordingFetcher fetcher = new RecordingFetcher();
        fetcher.register("https://example.test/base/index.json", "{\"groups\":["
            + "{\"label\":\"groupOne\",\"url\":\"groupOne.json\"},"
            + "{\"label\":\"groupTwo\",\"url\":\"groupTwo.json\"}"
            + "]}");
        fetcher.register("https://example.test/base/groupOne.json", "{\"entries\":["
            + "{\"label\":\"entryA\",\"urls\":[\"https://example.test/a\"]},"
            + "{\"label\":\"entryB\",\"urls\":[\"https://example.test/b\"]}"
            + "]}");
        fetcher.register("https://example.test/base/groupTwo.json", "{\"entries\":["
            + "{\"label\":\"entryC\",\"urls\":[\"https://example.test/c\"]}"
            + "]}");

        SessionDefinitionLoader loader =
            new SessionDefinitionLoader(fetcher, new SessionDefinitionParser());

        List<SessionDefinitionEntry> entries = loader.load("https://example.test/base/index.json");

        Assert.assertEquals(3, entries.size());
        Assert.assertEquals("groupOne/entryA", entries.get(0).getSessionName());
        Assert.assertEquals("groupOne/entryB", entries.get(1).getSessionName());
        Assert.assertEquals("groupTwo/entryC", entries.get(2).getSessionName());

        Assert.assertEquals("https://example.test/base/index.json", fetcher.fetchedUrls.get(0));
        Assert.assertEquals("https://example.test/base/groupOne.json", fetcher.fetchedUrls.get(1));
        Assert.assertEquals("https://example.test/base/groupTwo.json", fetcher.fetchedUrls.get(2));
    }

    @Test(expected = IOException.class)
    public void loadPropagatesFetchFailure() throws Exception {
        RecordingFetcher fetcher = new RecordingFetcher();
        SessionDefinitionLoader loader =
            new SessionDefinitionLoader(fetcher, new SessionDefinitionParser());
        loader.load("https://example.test/missing/index.json");
    }
}
