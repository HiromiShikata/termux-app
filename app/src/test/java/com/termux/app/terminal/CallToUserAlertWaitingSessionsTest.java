package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CallToUserAlertWaitingSessionsTest {

    private static final long CALLED_AT_MILLIS = 1_000L;

    private static final class Case {
        final String description;
        final Long calledAtMillis;
        final Long genuineReplyMillis;
        final boolean storePendingNow;
        final boolean expectWaiting;

        Case(String description, Long calledAtMillis, Long genuineReplyMillis, boolean storePendingNow,
             boolean expectWaiting) {
            this.description = description;
            this.calledAtMillis = calledAtMillis;
            this.genuineReplyMillis = genuineReplyMillis;
            this.storePendingNow = storePendingNow;
            this.expectWaiting = expectWaiting;
        }
    }

    @Test
    public void alertStaysWhileTheOwnerHasNotAnsweredTheCallWhateverTheStoreSaysAboutKeystrokes() {
        Case[] cases = {
            new Case("typing cleared the store pending set but no reply came", CALLED_AT_MILLIS, null,
                false, true),
            new Case("a reply before the call does not answer it", CALLED_AT_MILLIS, CALLED_AT_MILLIS - 1,
                true, true),
            new Case("a reply at the call time answers it", CALLED_AT_MILLIS, CALLED_AT_MILLIS, true, false),
            new Case("a reply after the call answers it", CALLED_AT_MILLIS, CALLED_AT_MILLIS + 5, false, false),
            new Case("no call time recorded falls back to the store pending set", null, null, true, true),
            new Case("no call time recorded and nothing pending means not waiting", null, null, false, false),
        };
        for (Case testCase : cases) {
            Map<String, Long> calledAt = new HashMap<>();
            if (testCase.calledAtMillis != null) {
                calledAt.put("worker", testCase.calledAtMillis);
            }
            Map<String, Long> genuineReply = new HashMap<>();
            if (testCase.genuineReplyMillis != null) {
                genuineReply.put("worker", testCase.genuineReplyMillis);
            }
            Set<String> storePending = testCase.storePendingNow
                ? Collections.singleton("worker") : Collections.<String>emptySet();

            Set<String> waiting = CallToUserAlertWaitingSessions.of(calledAt, storePending, genuineReply);

            Assert.assertEquals(testCase.description, testCase.expectWaiting, waiting.contains("worker"));
        }
    }

    @Test
    public void sessionsWithoutAnAlertInTheCalledAtMapFollowTheStorePendingSet() {
        Set<String> waiting = CallToUserAlertWaitingSessions.of(Collections.<String, Long>emptyMap(),
            new HashSet<>(Arrays.asList("first", "second")), Collections.<String, Long>emptyMap());

        Assert.assertEquals(new HashSet<>(Arrays.asList("first", "second")), waiting);
    }
}
