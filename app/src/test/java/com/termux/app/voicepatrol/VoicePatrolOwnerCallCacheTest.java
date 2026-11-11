package com.termux.app.voicepatrol;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.termux.app.ownercall.OwnerCall;
import com.termux.app.ownercall.OwnerCallInbox;

import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

public class VoicePatrolOwnerCallCacheTest {

    private static final long INTERVAL = OwnerCallInbox.MINIMUM_READ_INTERVAL_MILLIS;
    private static final String FIRST_URL = "https://calls.example.test/test/voicetest1.yaml?k=t";
    private static final String SECOND_URL = "https://calls.example.test/test/voicetest2.yaml?k=t";

    private static final class RecordingSource implements VoicePatrolOwnerCallCache.CallSource {
        final List<String> readUrls = new ArrayList<>();
        final Map<String, List<OwnerCall>> callsByUrl = new HashMap<>();
        final Set<String> failingUrls = new HashSet<>();

        @Override
        public List<OwnerCall> readCalls(String fileUrl, String sessionName) throws Exception {
            readUrls.add(fileUrl);
            if (failingUrls.contains(fileUrl)) {
                throw new IOException("unreachable " + sessionName);
            }
            List<OwnerCall> calls = callsByUrl.get(fileUrl);
            return calls == null ? Collections.<OwnerCall>emptyList() : calls;
        }
    }

    private static final class RecordingLog implements VoicePatrolLog {
        final List<String> warnings = new ArrayList<>();

        @Override
        public void warn(String message) {
            warnings.add(message);
        }
    }

    private static final class QueuedExecutor implements Executor {
        final List<Runnable> queued = new ArrayList<>();

        @Override
        public void execute(Runnable command) {
            queued.add(command);
        }

        void runAll() {
            List<Runnable> pending = new ArrayList<>(queued);
            queued.clear();
            for (Runnable command : pending) {
                command.run();
            }
        }
    }

    private static OwnerCall call(String sessionName, String body) {
        return new OwnerCall(sessionName, "2026-11-11T12:50:00Z", body);
    }

    @Test
    public void returnsCallsReadForTheSessionAndReadsItOnlyOnceWithinTheRefreshInterval() {
        RecordingSource source = new RecordingSource();
        source.callsByUrl.put(FIRST_URL, Collections.singletonList(call("voicetest1", "first body")));
        VoicePatrolOwnerCallCache cache = new VoicePatrolOwnerCallCache(source, Runnable::run, Runnable::run,
            new RecordingLog());

        List<OwnerCall> firstRead = cache.callsOf("voicetest1", FIRST_URL, 1000L);
        List<OwnerCall> secondRead = cache.callsOf("voicetest1", FIRST_URL, 2000L);

        assertEquals(1, firstRead.size());
        assertEquals("first body", firstRead.get(0).getBody());
        assertEquals("first body", secondRead.get(0).getBody());
        assertEquals(1, source.readUrls.size());
    }

    @Test
    public void readsEachSessionFromItsOwnFileAddress() {
        RecordingSource source = new RecordingSource();
        source.callsByUrl.put(FIRST_URL, Collections.singletonList(call("voicetest1", "one")));
        source.callsByUrl.put(SECOND_URL, Collections.singletonList(call("voicetest2", "two")));
        VoicePatrolOwnerCallCache cache = new VoicePatrolOwnerCallCache(source, Runnable::run, Runnable::run,
            new RecordingLog());

        String firstBody = cache.callsOf("voicetest1", FIRST_URL, 0L).get(0).getBody();
        String secondBody = cache.callsOf("voicetest2", SECOND_URL, 0L).get(0).getBody();

        assertEquals("one", firstBody);
        assertEquals("two", secondBody);
        assertEquals(List.of(FIRST_URL, SECOND_URL), source.readUrls);
    }

    @Test
    public void readsAgainOnlyAfterTheRefreshIntervalHasPassed() {
        RecordingSource source = new RecordingSource();
        VoicePatrolOwnerCallCache cache = new VoicePatrolOwnerCallCache(source, Runnable::run, Runnable::run,
            new RecordingLog());

        cache.callsOf("voicetest1", FIRST_URL, 0L);
        cache.callsOf("voicetest1", FIRST_URL, INTERVAL - 1);
        assertEquals(1, source.readUrls.size());

        cache.callsOf("voicetest1", FIRST_URL, INTERVAL);
        assertEquals(2, source.readUrls.size());
    }

    @Test
    public void startsOneReadPerSessionWhileTheReadIsStillInFlight() {
        RecordingSource source = new RecordingSource();
        source.callsByUrl.put(FIRST_URL, Collections.singletonList(call("voicetest1", "late body")));
        QueuedExecutor background = new QueuedExecutor();
        VoicePatrolOwnerCallCache cache = new VoicePatrolOwnerCallCache(source, background, Runnable::run,
            new RecordingLog());

        List<OwnerCall> beforeRead = cache.callsOf("voicetest1", FIRST_URL, 0L);
        cache.callsOf("voicetest1", FIRST_URL, 1L);
        assertEquals(1, background.queued.size());
        assertTrue(beforeRead.isEmpty());

        background.runAll();

        assertEquals("late body", cache.callsOf("voicetest1", FIRST_URL, 2L).get(0).getBody());
        assertEquals(1, source.readUrls.size());
    }

    @Test
    public void failedReadIsLoggedAndRetriedOnlyAfterTheRefreshInterval() {
        RecordingSource source = new RecordingSource();
        source.failingUrls.add(FIRST_URL);
        RecordingLog log = new RecordingLog();
        VoicePatrolOwnerCallCache cache = new VoicePatrolOwnerCallCache(source, Runnable::run, Runnable::run, log);

        List<OwnerCall> failedRead = cache.callsOf("voicetest1", FIRST_URL, 0L);
        cache.callsOf("voicetest1", FIRST_URL, 1L);

        assertTrue(failedRead.isEmpty());
        assertEquals(1, source.readUrls.size());
        assertEquals(1, log.warnings.size());
        assertTrue(log.warnings.get(0).contains("voicetest1"));

        source.failingUrls.clear();
        cache.callsOf("voicetest1", FIRST_URL, INTERVAL);
        assertEquals(2, source.readUrls.size());
    }

    @Test
    public void failedReadKeepsCallsReadEarlierFromTheSameFile() {
        RecordingSource source = new RecordingSource();
        source.callsByUrl.put(FIRST_URL, Collections.singletonList(call("voicetest1", "kept body")));
        RecordingLog log = new RecordingLog();
        VoicePatrolOwnerCallCache cache = new VoicePatrolOwnerCallCache(source, Runnable::run, Runnable::run, log);
        cache.callsOf("voicetest1", FIRST_URL, 0L);

        source.failingUrls.add(FIRST_URL);
        List<OwnerCall> afterFailure = cache.callsOf("voicetest1", FIRST_URL, INTERVAL);

        assertEquals("kept body", afterFailure.get(0).getBody());
        assertEquals(1, log.warnings.size());
    }

    @Test
    public void forgetsSessionsThatAreNoLongerCallingSoTheyAreReadAgainWhenTheyCallAgain() {
        RecordingSource source = new RecordingSource();
        VoicePatrolOwnerCallCache cache = new VoicePatrolOwnerCallCache(source, Runnable::run, Runnable::run,
            new RecordingLog());
        cache.callsOf("voicetest1", FIRST_URL, 0L);
        cache.callsOf("voicetest2", SECOND_URL, 0L);

        cache.retainOnly(Collections.singleton("voicetest2"));
        cache.callsOf("voicetest1", FIRST_URL, 1L);
        cache.callsOf("voicetest2", SECOND_URL, 1L);

        assertEquals(List.of(FIRST_URL, SECOND_URL, FIRST_URL), source.readUrls);
    }

    @Test
    public void nullFileUrlReadsNothingAndReturnsNoCallsOnTheFirstPoll() {
        RecordingSource source = new RecordingSource();
        VoicePatrolOwnerCallCache cache = new VoicePatrolOwnerCallCache(source, Runnable::run, Runnable::run,
            new RecordingLog());

        List<OwnerCall> calls = cache.callsOf("voicetest1", null, 0L);

        assertTrue(calls.isEmpty());
        assertTrue(source.readUrls.isEmpty());
    }

    @Test
    public void nullFileUrlOnLaterPollsStillReadsNothingAndDoesNotThrow() {
        RecordingSource source = new RecordingSource();
        source.callsByUrl.put(FIRST_URL, Collections.singletonList(call("voicetest1", "before")));
        VoicePatrolOwnerCallCache cache = new VoicePatrolOwnerCallCache(source, Runnable::run, Runnable::run,
            new RecordingLog());
        cache.callsOf("voicetest1", FIRST_URL, 0L);

        List<OwnerCall> firstNullPoll = cache.callsOf("voicetest1", null, 1L);
        List<OwnerCall> secondNullPoll = cache.callsOf("voicetest1", null, 2L);

        assertTrue(firstNullPoll.isEmpty());
        assertTrue(secondNullPoll.isEmpty());
        assertEquals(List.of(FIRST_URL), source.readUrls);
    }

    @Test
    public void fileUrlBecomingNullDropsTheCallsAndReadsAgainWhenTheUrlReturns() {
        RecordingSource source = new RecordingSource();
        source.callsByUrl.put(FIRST_URL, Collections.singletonList(call("voicetest1", "kept")));
        VoicePatrolOwnerCallCache cache = new VoicePatrolOwnerCallCache(source, Runnable::run, Runnable::run,
            new RecordingLog());
        cache.callsOf("voicetest1", FIRST_URL, 0L);

        List<OwnerCall> afterUrlLost = cache.callsOf("voicetest1", null, 1L);
        List<OwnerCall> afterUrlBack = cache.callsOf("voicetest1", FIRST_URL, 2L);

        assertTrue(afterUrlLost.isEmpty());
        assertEquals("kept", afterUrlBack.get(0).getBody());
        assertEquals(List.of(FIRST_URL, FIRST_URL), source.readUrls);
    }

    @Test
    public void afterCloseNoCallsAreReturnedAndNothingIsRead() {
        RecordingSource source = new RecordingSource();
        source.callsByUrl.put(FIRST_URL, Collections.singletonList(call("voicetest1", "body")));
        VoicePatrolOwnerCallCache cache = new VoicePatrolOwnerCallCache(source, Runnable::run, Runnable::run,
            new RecordingLog());

        cache.close();
        List<OwnerCall> afterClose = cache.callsOf("voicetest1", FIRST_URL, 0L);

        assertTrue(afterClose.isEmpty());
        assertTrue(source.readUrls.isEmpty());
    }

    @Test
    public void readCompletingAfterCloseIsIgnoredAndLogsNothing() {
        RecordingSource source = new RecordingSource();
        source.failingUrls.add(FIRST_URL);
        RecordingLog log = new RecordingLog();
        QueuedExecutor background = new QueuedExecutor();
        VoicePatrolOwnerCallCache cache = new VoicePatrolOwnerCallCache(source, background, Runnable::run, log);
        cache.callsOf("voicetest1", FIRST_URL, 0L);

        cache.close();
        background.runAll();

        assertEquals(1, source.readUrls.size());
        assertTrue(log.warnings.isEmpty());
    }

    @Test
    public void closeDropsCachedCallsSoNothingSurvivesTheActivity() {
        RecordingSource source = new RecordingSource();
        source.callsByUrl.put(FIRST_URL, Collections.singletonList(call("voicetest1", "body")));
        VoicePatrolOwnerCallCache cache = new VoicePatrolOwnerCallCache(source, Runnable::run, Runnable::run,
            new RecordingLog());
        cache.callsOf("voicetest1", FIRST_URL, 0L);

        cache.close();

        assertTrue(cache.callsOf("voicetest1", FIRST_URL, 1L).isEmpty());
        assertEquals(1, source.readUrls.size());
    }
}
