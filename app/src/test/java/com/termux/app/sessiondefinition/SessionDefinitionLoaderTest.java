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
        fetcher.register("https://example.test/base/index.json",
            "{\"projects\":[\"groupOne\",\"groupTwo\"]}");
        fetcher.register("https://example.test/base/groupOne.json", "["
            + "{\"story\":\"entryA\",\"urls\":[\"https://example.test/a\"]},"
            + "{\"story\":\"entryB\",\"urls\":[\"https://example.test/b\"]}"
            + "]");
        fetcher.register("https://example.test/base/groupTwo.json", "["
            + "{\"story\":\"entryC\",\"urls\":[\"https://example.test/c\"]}"
            + "]");

        SessionDefinitionLoader loader =
            new SessionDefinitionLoader(fetcher, new SessionDefinitionParser());

        List<SessionDefinitionEntry> entries =
            loader.load("https://example.test/base/index.json").getEntries();

        Assert.assertEquals(3, entries.size());
        Assert.assertEquals("groupOne/entryA", entries.get(0).getSessionName());
        Assert.assertEquals("groupOne/entryB", entries.get(1).getSessionName());
        Assert.assertEquals("groupTwo/entryC", entries.get(2).getSessionName());

        Assert.assertEquals("https://example.test/base/index.json", fetcher.fetchedUrls.get(0));
        Assert.assertEquals("https://example.test/base/groupOne.json", fetcher.fetchedUrls.get(1));
        Assert.assertEquals("https://example.test/base/groupTwo.json", fetcher.fetchedUrls.get(2));
    }

    @Test
    public void loadVersionTwoIndexResolvesVersionTwoProjectFilesAndCarriesTitles() throws Exception {
        RecordingFetcher fetcher = new RecordingFetcher();
        fetcher.register("https://example.test/base/index.v2.json",
            "{\"version\":2,\"projects\":[\"alpha\"]}");
        fetcher.register("https://example.test/base/alpha.v2.json", "["
            + "{\"story\":\"story-one\",\"urls\":["
            + "{\"url\":\"https://example.test/a\",\"title\":\"Task A\"},"
            + "\"https://example.test/b\""
            + "]}"
            + "]");

        SessionDefinitionLoader loader =
            new SessionDefinitionLoader(fetcher, new SessionDefinitionParser());

        List<SessionDefinitionEntry> entries =
            loader.load("https://example.test/base/index.v2.json").getEntries();

        Assert.assertEquals(1, entries.size());
        SessionDefinitionEntry entry = entries.get(0);
        Assert.assertEquals("alpha/story-one", entry.getSessionName());
        Assert.assertEquals("Task A", entry.getTitleForUrl("https://example.test/a"));
        Assert.assertNull(entry.getTitleForUrl("https://example.test/b"));

        Assert.assertEquals("https://example.test/base/index.v2.json", fetcher.fetchedUrls.get(0));
        Assert.assertEquals("https://example.test/base/alpha.v2.json", fetcher.fetchedUrls.get(1));
    }

    @Test
    public void loadVersionThreeIndexResolvesVersionThreeProjectFilesAndCarriesOverviewUrl() throws Exception {
        RecordingFetcher fetcher = new RecordingFetcher();
        fetcher.register("https://example.test/base/index.v3.json",
            "{\"version\":3,\"projects\":[\"umino\",\"xmile\"]}");
        fetcher.register("https://example.test/base/umino.v3.json", "{"
            + "\"version\":3,"
            + "\"overviewUrl\":\"https://github.com/HiromiShikata/projects/7\","
            + "\"groups\":["
            + "{\"story\":\"story-one\",\"urls\":["
            + "{\"url\":\"https://example.test/a\",\"title\":\"Task A\"}"
            + "]}"
            + "]}");
        fetcher.register("https://example.test/base/xmile.v3.json", "{"
            + "\"version\":3,"
            + "\"groups\":["
            + "{\"story\":\"story-two\",\"urls\":[\"https://example.test/b\"]}"
            + "]}");

        SessionDefinitionLoader loader =
            new SessionDefinitionLoader(fetcher, new SessionDefinitionParser());

        List<SessionDefinitionEntry> entries =
            loader.load("https://example.test/base/index.v3.json").getEntries();

        Assert.assertEquals(2, entries.size());
        SessionDefinitionEntry uminoEntry = entries.get(0);
        Assert.assertEquals("umino/story-one", uminoEntry.getSessionName());
        Assert.assertEquals("https://github.com/HiromiShikata/projects/7", uminoEntry.getOverviewUrl());
        Assert.assertEquals("Task A", uminoEntry.getTitleForUrl("https://example.test/a"));
        SessionDefinitionEntry xmileEntry = entries.get(1);
        Assert.assertEquals("xmile/story-two", xmileEntry.getSessionName());
        Assert.assertNull(xmileEntry.getOverviewUrl());

        Assert.assertEquals("https://example.test/base/index.v3.json", fetcher.fetchedUrls.get(0));
        Assert.assertEquals("https://example.test/base/umino.v3.json", fetcher.fetchedUrls.get(1));
        Assert.assertEquals("https://example.test/base/xmile.v3.json", fetcher.fetchedUrls.get(2));
    }

    @Test(expected = IOException.class)
    public void loadPropagatesIndexFetchFailure() throws Exception {
        RecordingFetcher fetcher = new RecordingFetcher();
        SessionDefinitionLoader loader =
            new SessionDefinitionLoader(fetcher, new SessionDefinitionParser());
        loader.load("https://example.test/missing/index.json");
    }

    @Test
    public void loadSkipsGroupThatFailsToFetchAndKeepsRemainingGroups() throws Exception {
        RecordingFetcher fetcher = new RecordingFetcher();
        fetcher.register("https://example.test/base/index.json",
            "{\"projects\":[\"groupOne\",\"groupMissing\",\"groupThree\"]}");
        fetcher.register("https://example.test/base/groupOne.json",
            "[{\"story\":\"entryA\",\"urls\":[\"https://example.test/a\"]}]");
        fetcher.register("https://example.test/base/groupThree.json",
            "[{\"story\":\"entryC\",\"urls\":[\"https://example.test/c\"]}]");

        SessionDefinitionLoader loader =
            new SessionDefinitionLoader(fetcher, new SessionDefinitionParser());

        SessionDefinitionLoadResult result = loader.load("https://example.test/base/index.json");

        Assert.assertEquals(2, result.getEntries().size());
        Assert.assertEquals("groupOne/entryA", result.getEntries().get(0).getSessionName());
        Assert.assertEquals("groupThree/entryC", result.getEntries().get(1).getSessionName());
        Assert.assertEquals(3, result.getTotalGroupCount());
        Assert.assertEquals(1, result.getFailedGroupCount());
        Assert.assertTrue(result.hasFailedGroups());
        Assert.assertEquals("groupMissing", result.getFailedGroupLabels().get(0));
    }

    @Test
    public void loadSkipsGroupWithMalformedJsonAndKeepsRemainingGroups() throws Exception {
        RecordingFetcher fetcher = new RecordingFetcher();
        fetcher.register("https://example.test/base/index.json",
            "{\"projects\":[\"groupBroken\",\"groupGood\"]}");
        fetcher.register("https://example.test/base/groupBroken.json", "{not valid json");
        fetcher.register("https://example.test/base/groupGood.json",
            "[{\"story\":\"entryB\",\"urls\":[\"https://example.test/b\"]}]");

        SessionDefinitionLoader loader =
            new SessionDefinitionLoader(fetcher, new SessionDefinitionParser());

        SessionDefinitionLoadResult result = loader.load("https://example.test/base/index.json");

        Assert.assertEquals(1, result.getEntries().size());
        Assert.assertEquals("groupGood/entryB", result.getEntries().get(0).getSessionName());
        Assert.assertEquals(1, result.getFailedGroupCount());
        Assert.assertEquals("groupBroken", result.getFailedGroupLabels().get(0));
    }

    @Test
    public void loadReportsNoFailedGroupsWhenAllGroupsLoad() throws Exception {
        RecordingFetcher fetcher = new RecordingFetcher();
        fetcher.register("https://example.test/base/index.json", "{\"projects\":[\"groupOne\"]}");
        fetcher.register("https://example.test/base/groupOne.json",
            "[{\"story\":\"entryA\",\"urls\":[\"https://example.test/a\"]}]");

        SessionDefinitionLoader loader =
            new SessionDefinitionLoader(fetcher, new SessionDefinitionParser());

        SessionDefinitionLoadResult result = loader.load("https://example.test/base/index.json");

        Assert.assertFalse(result.hasFailedGroups());
        Assert.assertEquals(0, result.getFailedGroupCount());
        Assert.assertEquals(1, result.getTotalGroupCount());
    }
}
