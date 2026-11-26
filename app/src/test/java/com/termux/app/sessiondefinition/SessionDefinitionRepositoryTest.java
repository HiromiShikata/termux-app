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
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(RobolectricTestRunner.class)
public class SessionDefinitionRepositoryTest {

    private static final String INDEX_URL = "https://example.test/base/index.json";

    private static final class CountingFetcher implements SessionDefinitionDocumentFetcher {
        private final Map<String, String> documents = new HashMap<>();
        private final AtomicInteger indexFetchCount = new AtomicInteger();
        private boolean fail;
        private boolean failGroupTwo;

        void register(String url, String document) {
            documents.put(url, document);
        }

        @Override
        public String fetch(String url) throws IOException {
            if (fail) {
                throw new IOException("boom");
            }
            if (failGroupTwo && url.equals("https://example.test/base/groupTwo.json")) {
                throw new IOException("groupTwo unavailable");
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

    private SessionDefinitionRepository repositoryWith(SessionDefinitionDocumentFetcher fetcher) {
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

    @Test
    public void loadCallsListenerImmediatelyWhileForcedRefreshIsInFlight() throws Exception {
        SequencedBlockingFetcher fetcher = new SequencedBlockingFetcher(2);
        SessionDefinitionRepository repository = repositoryWith(fetcher);

        repository.load(INDEX_URL, () -> {});
        flushMainLooper();
        assertEquals(1, fetcher.fetchCount(INDEX_URL));

        repository.loadForRebuild(INDEX_URL, true, (result) -> {}, (exception) -> {});
        assertTrue(fetcher.blockedIndexFetchStarted.await(5, TimeUnit.SECONDS));

        AtomicInteger listenerCalls = new AtomicInteger();
        repository.load(INDEX_URL, listenerCalls::incrementAndGet);

        assertEquals(1, listenerCalls.get());
        fetcher.releaseBlockedIndexFetch.countDown();
        flushMainLooper();
        assertEquals(2, fetcher.fetchCount(INDEX_URL));
    }

    @Test
    public void forcedLoadDuringInFlightFetchReusesIt() throws Exception {
        SequencedBlockingFetcher fetcher = new SequencedBlockingFetcher(1);
        SessionDefinitionRepository repository = repositoryWith(fetcher);

        repository.load(INDEX_URL, () -> {});
        assertTrue(fetcher.blockedIndexFetchStarted.await(5, TimeUnit.SECONDS));

        AtomicReference<SessionDefinitionLoadResult> forcedResult = new AtomicReference<>();
        repository.loadForRebuild(INDEX_URL, true, forcedResult::set, (exception) -> {});

        fetcher.releaseBlockedIndexFetch.countDown();
        flushMainLooper();

        assertEquals(1, fetcher.fetchCount(INDEX_URL));
        assertSessionNames(forcedResult.get().getEntries(), "first/firstStory");
    }

    @Test
    public void forcedLoadsWhileInitialFetchIsInFlightShareOneFetch() throws Exception {
        SequencedBlockingFetcher fetcher = new SequencedBlockingFetcher(1);
        SessionDefinitionRepository repository = repositoryWith(fetcher);

        repository.load(INDEX_URL, () -> {});
        assertTrue(fetcher.blockedIndexFetchStarted.await(5, TimeUnit.SECONDS));

        AtomicReference<SessionDefinitionLoadResult> firstForcedResult = new AtomicReference<>();
        AtomicReference<SessionDefinitionLoadResult> secondForcedResult = new AtomicReference<>();
        repository.loadForRebuild(INDEX_URL, true, firstForcedResult::set, (exception) -> {});
        repository.loadForRebuild(INDEX_URL, true, secondForcedResult::set, (exception) -> {});

        fetcher.releaseBlockedIndexFetch.countDown();
        flushMainLooper();

        assertEquals(1, fetcher.fetchCount(INDEX_URL));
        assertSessionNames(firstForcedResult.get().getEntries(), "first/firstStory");
        assertSessionNames(secondForcedResult.get().getEntries(), "first/firstStory");
    }

    @Test
    public void partialForceRefreshMergesSuccessfulGroupsWithPreviousFailedGroupEntries() throws Exception {
        CountingFetcher fetcher = fetcherWithTwoEntries();
        SessionDefinitionRepository repository = repositoryWith(fetcher);

        repository.loadForRebuild(INDEX_URL, (entries) -> {}, (exception) -> {});
        flushMainLooper();

        fetcher.register("https://example.test/base/groupOne.json",
            "[{\"story\":\"storyA-new\",\"urls\":[\"https://example.test/new\"]}]");
        fetcher.failGroupTwo = true;

        AtomicReference<SessionDefinitionLoadResult> partial = new AtomicReference<>();
        repository.loadForRebuild(INDEX_URL, true, partial::set, (exception) -> {});
        flushMainLooper();

        assertEquals(Collections.singletonList("groupTwo"), partial.get().getFailedGroupLabels());
        assertSessionNames(repository.getCachedEntries(),
            "groupOne/storyA-new", "groupTwo/storyB");
    }

    @Test
    public void concurrentForcedLoadsShareOneFetchAndNotifyBothListeners() throws Exception {
        BlockingFetcher fetcher = new BlockingFetcher(fetcherWithTwoEntries());
        SessionDefinitionRepository repository = repositoryWith(fetcher);
        AtomicInteger firstListenerCalls = new AtomicInteger();
        AtomicInteger secondListenerCalls = new AtomicInteger();

        repository.loadForRebuild(INDEX_URL, true,
            result -> firstListenerCalls.incrementAndGet(), exception -> {});
        assertTrue(fetcher.indexFetchStarted.await(5, TimeUnit.SECONDS));
        repository.loadForRebuild(INDEX_URL, true,
            result -> secondListenerCalls.incrementAndGet(), exception -> {});

        fetcher.releaseIndexFetch.countDown();
        flushMainLooper();

        assertEquals(1, fetcher.fetchCount(INDEX_URL));
        assertEquals(1, fetcher.fetchCount("https://example.test/base/groupOne.json"));
        assertEquals(1, fetcher.fetchCount("https://example.test/base/groupTwo.json"));
        assertEquals(1, firstListenerCalls.get());
        assertEquals(1, secondListenerCalls.get());
    }

    private static void assertSessionNames(List<SessionDefinitionEntry> entries, String... expected) {
        List<String> sessionNames = new java.util.ArrayList<>();
        for (SessionDefinitionEntry entry : entries) {
            sessionNames.add(entry.getSessionName());
        }
        assertEquals(java.util.Arrays.asList(expected), sessionNames);
    }

    private static final class BlockingFetcher implements SessionDefinitionDocumentFetcher {
        private final CountingFetcher delegate;
        private final CountDownLatch indexFetchStarted = new CountDownLatch(1);
        private final CountDownLatch releaseIndexFetch = new CountDownLatch(1);
        private final Map<String, AtomicInteger> fetchCounts = new ConcurrentHashMap<>();

        BlockingFetcher(CountingFetcher delegate) {
            this.delegate = delegate;
        }

        @Override
        public String fetch(String url) throws IOException {
            fetchCounts.computeIfAbsent(url, ignored -> new AtomicInteger()).incrementAndGet();
            if (url.equals(INDEX_URL) && indexFetchStarted.getCount() > 0) {
                indexFetchStarted.countDown();
                try {
                    releaseIndexFetch.await();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IOException(exception);
                }
            }
            return delegate.fetch(url);
        }

        int fetchCount(String url) {
            AtomicInteger count = fetchCounts.get(url);
            return count == null ? 0 : count.get();
        }
    }

    private static final class SequencedBlockingFetcher implements SessionDefinitionDocumentFetcher {
        private final int blockedIndexFetchNumber;
        private final CountDownLatch blockedIndexFetchStarted = new CountDownLatch(1);
        private final CountDownLatch releaseBlockedIndexFetch = new CountDownLatch(1);
        private final Map<String, AtomicInteger> fetchCounts = new ConcurrentHashMap<>();

        SequencedBlockingFetcher(int blockedIndexFetchNumber) {
            this.blockedIndexFetchNumber = blockedIndexFetchNumber;
        }

        @Override
        public String fetch(String url) throws IOException {
            int fetchCount = fetchCounts.computeIfAbsent(url, ignored -> new AtomicInteger())
                .incrementAndGet();
            if (url.equals(INDEX_URL)) {
                if (fetchCount == blockedIndexFetchNumber) {
                    blockedIndexFetchStarted.countDown();
                    try {
                        releaseBlockedIndexFetch.await();
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                        throw new IOException(exception);
                    }
                }
                return fetchCount == 1
                    ? "{\"projects\":[\"first\"]}"
                    : "{\"projects\":[\"second\"]}";
            }
            if (url.equals("https://example.test/base/first.json")) {
                return "[{\"story\":\"firstStory\",\"urls\":[\"https://example.test/first\"]}]";
            }
            if (url.equals("https://example.test/base/second.json")) {
                return "[{\"story\":\"secondStory\",\"urls\":[\"https://example.test/second\"]}]";
            }
            throw new IOException("No document registered for " + url);
        }

        int fetchCount(String url) {
            AtomicInteger count = fetchCounts.get(url);
            return count == null ? 0 : count.get();
        }
    }
}
