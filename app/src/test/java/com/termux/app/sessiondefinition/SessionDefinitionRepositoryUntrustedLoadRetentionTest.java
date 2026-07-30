package com.termux.app.sessiondefinition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.os.Looper;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(RobolectricTestRunner.class)
public class SessionDefinitionRepositoryUntrustedLoadRetentionTest {

    private static final String INDEX_URL = "https://example.test/base/index.json";
    private static final String FIRST_GROUP_URL = "https://example.test/base/groupOne.json";
    private static final String SECOND_GROUP_URL = "https://example.test/base/groupTwo.json";

    private static final class SelectivelyFailingFetcher implements SessionDefinitionDocumentFetcher {
        private final Map<String, String> documents = new HashMap<>();
        private final Set<String> failingUrls = new HashSet<>();

        void register(String url, String document) {
            documents.put(url, document);
        }

        void failFor(String url) {
            failingUrls.add(url);
        }

        @Override
        public String fetch(String url) throws IOException {
            if (failingUrls.contains(url)) {
                throw new IOException("Unexpected HTTP response code 502 for " + url);
            }
            String document = documents.get(url);
            if (document == null) {
                throw new IOException("No document registered for " + url);
            }
            return document;
        }
    }

    private static SelectivelyFailingFetcher fetcherWithTwoEntries() {
        SelectivelyFailingFetcher fetcher = new SelectivelyFailingFetcher();
        fetcher.register(INDEX_URL, "{\"projects\":[\"groupOne\",\"groupTwo\"]}");
        fetcher.register(FIRST_GROUP_URL, "[{\"story\":\"storyA\",\"urls\":[\"https://example.test/a\"]}]");
        fetcher.register(SECOND_GROUP_URL, "[{\"story\":\"storyB\",\"urls\":[\"https://example.test/b\"]}]");
        return fetcher;
    }

    private SessionDefinitionRepository repositoryWith(SelectivelyFailingFetcher fetcher) {
        return new SessionDefinitionRepository(
            new SessionDefinitionLoader(fetcher, new SessionDefinitionParser()));
    }

    private void flushMainLooper() throws InterruptedException {
        for (int attempt = 0; attempt < 200; attempt++) {
            Shadows.shadowOf(Looper.getMainLooper()).idle();
            Thread.sleep(5);
        }
    }

    private SessionDefinitionRepository repositoryLoadedWithTwoEntries(SelectivelyFailingFetcher fetcher)
        throws Exception {
        SessionDefinitionRepository repository = repositoryWith(fetcher);
        repository.loadForRebuild(INDEX_URL, (result) -> {}, (exception) -> {});
        flushMainLooper();
        assertEquals(2, repository.getCachedEntries().size());
        return repository;
    }

    @Test
    public void indexDeclaringNoProjectsKeepsThePopulatedCache() throws Exception {
        SelectivelyFailingFetcher fetcher = fetcherWithTwoEntries();
        SessionDefinitionRepository repository = repositoryLoadedWithTwoEntries(fetcher);

        fetcher.register(INDEX_URL, "{\"projects\":[]}");
        AtomicReference<SessionDefinitionLoadResult> delivered = new AtomicReference<>();
        repository.loadForRebuild(INDEX_URL, true, delivered::set, (exception) -> {});
        flushMainLooper();

        assertTrue(delivered.get().getEntries().isEmpty());
        assertEquals(2, repository.getCachedEntries().size());
    }

    @Test
    public void groupsDeclaringNoSessionsKeepThePopulatedCache() throws Exception {
        SelectivelyFailingFetcher fetcher = fetcherWithTwoEntries();
        SessionDefinitionRepository repository = repositoryLoadedWithTwoEntries(fetcher);

        fetcher.register(FIRST_GROUP_URL, "[]");
        fetcher.register(SECOND_GROUP_URL, "[]");
        AtomicReference<SessionDefinitionLoadResult> delivered = new AtomicReference<>();
        repository.loadForRebuild(INDEX_URL, true, delivered::set, (exception) -> {});
        flushMainLooper();

        assertTrue(delivered.get().getEntries().isEmpty());
        assertEquals(2, repository.getCachedEntries().size());
    }

    @Test
    public void partialLoadKeepsTheCacheFromTheLastAuthoritativeLoad() throws Exception {
        SelectivelyFailingFetcher fetcher = fetcherWithTwoEntries();
        SessionDefinitionRepository repository = repositoryLoadedWithTwoEntries(fetcher);

        fetcher.failFor(SECOND_GROUP_URL);
        AtomicReference<SessionDefinitionLoadResult> delivered = new AtomicReference<>();
        repository.loadForRebuild(INDEX_URL, true, delivered::set, (exception) -> {});
        flushMainLooper();

        assertTrue(delivered.get().hasFailedGroups());
        assertEquals(1, delivered.get().getEntries().size());
        assertEquals(2, repository.getCachedEntries().size());
    }
}
