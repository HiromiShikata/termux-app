package com.termux.app.sessiondefinition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.os.Looper;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(RobolectricTestRunner.class)
public class SessionDefinitionRepositoryTest {

    private static final String INDEX_URL = "https://example.test/base/index.json";

    private static final class CountingFetcher implements SessionDefinitionDocumentFetcher {
        private final Map<String, String> documents = new HashMap<>();
        private final AtomicInteger indexFetchCount = new AtomicInteger();
        private boolean fail;

        void register(String url, String document) {
            documents.put(url, document);
        }

        @Override
        public String fetch(String url) throws IOException {
            if (fail) {
                throw new IOException("boom");
            }
            if (url.equals(INDEX_URL)) {
                indexFetchCount.incrementAndGet();
            }
            String document = documents.get(url);
            if (document == null) {
                throw new IOException("No document registered for " + url);
            }
            return document;
        }
    }

    private static CountingFetcher fetcherWithTwoEntries() {
        CountingFetcher fetcher = new CountingFetcher();
        fetcher.register(INDEX_URL, "{\"projects\":[\"groupOne\",\"groupTwo\"]}");
        fetcher.register("https://example.test/base/groupOne.json",
            "[{\"story\":\"storyA\",\"urls\":[\"https://example.test/a\"]}]");
        fetcher.register("https://example.test/base/groupTwo.json",
            "[{\"story\":\"storyB\",\"urls\":[\"https://example.test/b\"]}]");
        return fetcher;
    }

    private SessionDefinitionRepository repositoryWith(CountingFetcher fetcher) {
        return new SessionDefinitionRepository(
            new SessionDefinitionLoader(fetcher, new SessionDefinitionParser()));
    }

    private void flushMainLooper() throws InterruptedException {
        for (int attempt = 0; attempt < 200; attempt++) {
            Shadows.shadowOf(Looper.getMainLooper()).idle();
            Thread.sleep(5);
        }
    }

    @Test
    public void getCachedEntriesDefaultsToEmpty() {
        SessionDefinitionRepository repository = repositoryWith(fetcherWithTwoEntries());
        assertTrue(repository.getCachedEntries().isEmpty());
        assertFalse(repository.isLoaded());
    }

    @Test
    public void loadForRebuildPopulatesTheSharedSnapshot() throws Exception {
        CountingFetcher fetcher = fetcherWithTwoEntries();
        SessionDefinitionRepository repository = repositoryWith(fetcher);

        AtomicReference<SessionDefinitionLoadResult> delivered = new AtomicReference<>();
        repository.loadForRebuild(INDEX_URL, delivered::set, (exception) -> {});

        flushMainLooper();

        assertEquals(2, delivered.get().getEntries().size());
        assertTrue(repository.isLoaded());
        assertSame(delivered.get().getEntries(), repository.getCachedEntries());
        assertFalse(delivered.get().hasFailedGroups());
        assertEquals(1, fetcher.indexFetchCount.get());
    }

    @Test
    public void loadForRebuildReusesTheCachedSnapshotWithoutReloading() throws Exception {
        CountingFetcher fetcher = fetcherWithTwoEntries();
        SessionDefinitionRepository repository = repositoryWith(fetcher);

        repository.loadForRebuild(INDEX_URL, (entries) -> {}, (exception) -> {});
        flushMainLooper();

        AtomicReference<SessionDefinitionLoadResult> secondDelivery = new AtomicReference<>();
        repository.loadForRebuild(INDEX_URL, secondDelivery::set, (exception) -> {});
        flushMainLooper();

        assertSame(repository.getCachedEntries(), secondDelivery.get().getEntries());
        assertEquals(1, fetcher.indexFetchCount.get());
    }

    @Test
    public void loadSharesTheSameSnapshotConsumedByLoadForRebuild() throws Exception {
        CountingFetcher fetcher = fetcherWithTwoEntries();
        SessionDefinitionRepository repository = repositoryWith(fetcher);

        AtomicInteger listListenerCalls = new AtomicInteger();
        repository.load(INDEX_URL, listListenerCalls::incrementAndGet);
        flushMainLooper();

        assertEquals(1, listListenerCalls.get());
        assertEquals(2, repository.getCachedEntries().size());

        AtomicReference<SessionDefinitionLoadResult> rebuildDelivery = new AtomicReference<>();
        repository.loadForRebuild(INDEX_URL, rebuildDelivery::set, (exception) -> {});
        flushMainLooper();

        assertSame(repository.getCachedEntries(), rebuildDelivery.get().getEntries());
        assertEquals(1, fetcher.indexFetchCount.get());
    }

    @Test
    public void loadForRebuildReportsFailureWithoutMarkingLoaded() throws Exception {
        CountingFetcher fetcher = fetcherWithTwoEntries();
        fetcher.fail = true;
        SessionDefinitionRepository repository = repositoryWith(fetcher);

        AtomicReference<Exception> failure = new AtomicReference<>();
        repository.loadForRebuild(INDEX_URL, (entries) -> {}, failure::set);
        flushMainLooper();

        assertTrue(failure.get() instanceof IOException);
        assertFalse(repository.isLoaded());
        assertTrue(repository.getCachedEntries().isEmpty());
    }

    @Test
    public void forceRefreshReFetchesAndPicksUpNewlyAddedEntry() throws Exception {
        CountingFetcher fetcher = fetcherWithTwoEntries();
        SessionDefinitionRepository repository = repositoryWith(fetcher);

        repository.loadForRebuild(INDEX_URL, (entries) -> {}, (exception) -> {});
        flushMainLooper();
        assertEquals(1, fetcher.indexFetchCount.get());
        assertEquals(2, repository.getCachedEntries().size());

        fetcher.register(INDEX_URL, "{\"projects\":[\"groupOne\",\"groupTwo\",\"groupThree\"]}");
        fetcher.register("https://example.test/base/groupThree.json",
            "[{\"story\":\"storyC\",\"urls\":[\"https://example.test/c\"]}]");

        AtomicReference<SessionDefinitionLoadResult> refreshed = new AtomicReference<>();
        repository.loadForRebuild(INDEX_URL, true, refreshed::set, (exception) -> {});
        flushMainLooper();

        assertEquals(2, fetcher.indexFetchCount.get());
        assertEquals(3, refreshed.get().getEntries().size());
        assertEquals(3, repository.getCachedEntries().size());
    }

    @Test
    public void plainRebuildDoesNotReFetchAfterInitialLoad() throws Exception {
        CountingFetcher fetcher = fetcherWithTwoEntries();
        SessionDefinitionRepository repository = repositoryWith(fetcher);

        repository.loadForRebuild(INDEX_URL, (entries) -> {}, (exception) -> {});
        flushMainLooper();
        assertEquals(1, fetcher.indexFetchCount.get());

        fetcher.register(INDEX_URL, "{\"projects\":[\"groupOne\",\"groupTwo\",\"groupThree\"]}");
        fetcher.register("https://example.test/base/groupThree.json",
            "[{\"story\":\"storyC\",\"urls\":[\"https://example.test/c\"]}]");

        AtomicReference<SessionDefinitionLoadResult> rebuilt = new AtomicReference<>();
        repository.loadForRebuild(INDEX_URL, false, rebuilt::set, (exception) -> {});
        flushMainLooper();

        assertEquals(1, fetcher.indexFetchCount.get());
        assertEquals(2, rebuilt.get().getEntries().size());
    }

    @Test
    public void failedForceRefreshKeepsPriorCachedDocument() throws Exception {
        CountingFetcher fetcher = fetcherWithTwoEntries();
        SessionDefinitionRepository repository = repositoryWith(fetcher);

        AtomicReference<SessionDefinitionLoadResult> initial = new AtomicReference<>();
        repository.loadForRebuild(INDEX_URL, initial::set, (exception) -> {});
        flushMainLooper();
        List<SessionDefinitionEntry> cachedBeforeFailure = repository.getCachedEntries();
        assertEquals(2, cachedBeforeFailure.size());

        fetcher.fail = true;
        AtomicReference<Exception> failure = new AtomicReference<>();
        repository.loadForRebuild(INDEX_URL, true, (entries) -> {}, failure::set);
        flushMainLooper();

        assertTrue(failure.get() instanceof IOException);
        assertTrue(repository.isLoaded());
        assertSame(cachedBeforeFailure, repository.getCachedEntries());
        assertEquals(2, repository.getCachedEntries().size());
    }
}
