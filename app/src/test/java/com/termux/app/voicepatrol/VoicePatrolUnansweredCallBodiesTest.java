package com.termux.app.voicepatrol;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.termux.app.ownercall.OwnerCall;
import com.termux.app.ownercall.OwnerCallInbox;

import org.junit.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

public class VoicePatrolUnansweredCallBodiesTest {

    private static final String CALLED_AT = "2026-11-11T12:50:00Z";
    private static final long CALLED_AT_MILLIS = Instant.parse(CALLED_AT).toEpochMilli();
    private static final long NOW = CALLED_AT_MILLIS + 60_000L;
    private static final String VOICETEST1_URL = "https://calls.example.test/test/voicetest1.yaml?k=t";
    private static final String VOICETEST2_URL = "https://calls.example.test/test/voicetest2.yaml?k=t";

    private static final class RecordingSource implements VoicePatrolOwnerCallCache.CallSource {
        final List<String> readUrls = new ArrayList<>();
        final Map<String, List<OwnerCall>> callsByUrl = new HashMap<>();

        @Override
        public List<OwnerCall> readCalls(String fileUrl, String sessionName) {
            readUrls.add(fileUrl);
            List<OwnerCall> calls = callsByUrl.get(fileUrl);
            return calls == null ? Collections.<OwnerCall>emptyList() : calls;
        }
    }

    private static final Executor RUN_NOW = Runnable::run;

    private static VoicePatrolLog silentLog() {
        return message -> {
        };
    }

    private static Map<String, String> urls() {
        Map<String, String> urls = new HashMap<>();
        urls.put("voicetest1", VOICETEST1_URL);
        urls.put("voicetest2", VOICETEST2_URL);
        return urls;
    }

    private static VoicePatrolUnansweredCallBodies.AnsweredThroughMillisOf noReplies() {
        return sessionName -> null;
    }

    private static Map<String, String> bodies(Set<String> calling, VoicePatrolOwnerCallCache cache,
                                              Map<String, String> urls,
                                              VoicePatrolUnansweredCallBodies.AnsweredThroughMillisOf answeredThrough,
                                              long nowMillis) {
        return VoicePatrolUnansweredCallBodies.bodiesBySessionName(calling, cache,
            urls::get, answeredThrough, nowMillis);
    }

    @Test
    public void returnsTheBodyOfACallingSessionFromItsCachedCalls() {
        RecordingSource source = new RecordingSource();
        source.callsByUrl.put(VOICETEST1_URL,
            Collections.singletonList(new OwnerCall("voicetest1", CALLED_AT, "first body")));
        VoicePatrolOwnerCallCache cache = new VoicePatrolOwnerCallCache(source, RUN_NOW, RUN_NOW, silentLog());
        Set<String> calling = new HashSet<>(Collections.singletonList("voicetest1"));

        Map<String, String> result = bodies(calling, cache, urls(), noReplies(), NOW);

        assertEquals("first body", result.get("voicetest1"));
        assertEquals(1, result.size());
    }

    @Test
    public void callsAnsweredThroughTheirCalledTimeAreFilteredOut() {
        RecordingSource source = new RecordingSource();
        source.callsByUrl.put(VOICETEST1_URL,
            Collections.singletonList(new OwnerCall("voicetest1", CALLED_AT, "answered body")));
        VoicePatrolOwnerCallCache cache = new VoicePatrolOwnerCallCache(source, RUN_NOW, RUN_NOW, silentLog());
        Set<String> calling = new HashSet<>(Collections.singletonList("voicetest1"));

        Map<String, String> result = bodies(calling, cache, urls(),
            sessionName -> CALLED_AT_MILLIS, NOW);

        assertFalse(result.containsKey("voicetest1"));
    }

    @Test
    public void callsCalledAfterTheLastGenuineReplyStillCountAsUnanswered() {
        RecordingSource source = new RecordingSource();
        source.callsByUrl.put(VOICETEST1_URL,
            Collections.singletonList(new OwnerCall("voicetest1", CALLED_AT, "still open")));
        VoicePatrolOwnerCallCache cache = new VoicePatrolOwnerCallCache(source, RUN_NOW, RUN_NOW, silentLog());
        Set<String> calling = new HashSet<>(Collections.singletonList("voicetest1"));

        Map<String, String> result = bodies(calling, cache, urls(),
            sessionName -> CALLED_AT_MILLIS - 1_000L, NOW);

        assertEquals("still open", result.get("voicetest1"));
    }

    @Test
    public void sessionsNoLongerCallingAreForgottenSoTheyAreReadAgainWhenTheyCallAgain() {
        RecordingSource source = new RecordingSource();
        source.callsByUrl.put(VOICETEST1_URL,
            Collections.singletonList(new OwnerCall("voicetest1", CALLED_AT, "one")));
        source.callsByUrl.put(VOICETEST2_URL,
            Collections.singletonList(new OwnerCall("voicetest2", CALLED_AT, "two")));
        VoicePatrolOwnerCallCache cache = new VoicePatrolOwnerCallCache(source, RUN_NOW, RUN_NOW, silentLog());
        Map<String, String> urls = urls();

        bodies(new HashSet<>(List.of("voicetest1", "voicetest2")), cache, urls, noReplies(), NOW);
        Map<String, String> onlyFirstCalling = bodies(
            new HashSet<>(Collections.singletonList("voicetest1")), cache, urls, noReplies(), NOW);
        bodies(new HashSet<>(List.of("voicetest1", "voicetest2")), cache, urls, noReplies(), NOW);

        assertEquals(1, onlyFirstCalling.size());
        assertEquals(1, Collections.frequency(source.readUrls, VOICETEST1_URL));
        assertEquals(2, Collections.frequency(source.readUrls, VOICETEST2_URL));
    }

    @Test
    public void nullFileUrlYieldsNoBodyAndNoRead() {
        RecordingSource source = new RecordingSource();
        VoicePatrolOwnerCallCache cache = new VoicePatrolOwnerCallCache(source, RUN_NOW, RUN_NOW, silentLog());
        Set<String> calling = new HashSet<>(Collections.singletonList("voicetest1"));
        Map<String, String> noUrl = new HashMap<>();

        Map<String, String> result = bodies(calling, cache, noUrl, noReplies(), NOW);

        assertTrue(result.isEmpty());
        assertTrue(source.readUrls.isEmpty());
    }
}
