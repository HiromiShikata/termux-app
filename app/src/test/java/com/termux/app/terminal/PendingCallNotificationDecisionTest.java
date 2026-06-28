package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class PendingCallNotificationDecisionTest {

    private static Set<String> setOf(String... names) {
        return new HashSet<>(java.util.Arrays.asList(names));
    }

    @Test
    public void newCallNotifiesOnce() {
        PendingCallNotificationDecision decision =
            PendingCallNotificationDecision.decide(setOf("worker"), Collections.emptySet());

        Assert.assertEquals(setOf("worker"), decision.getNewlyPendingSessionNames());
        Assert.assertEquals(setOf("worker"), decision.getNextNotifiedSessionNames());
    }

    @Test
    public void sameCallDoesNotRepeat() {
        PendingCallNotificationDecision decision =
            PendingCallNotificationDecision.decide(setOf("worker"), setOf("worker"));

        Assert.assertTrue(decision.getNewlyPendingSessionNames().isEmpty());
        Assert.assertEquals(setOf("worker"), decision.getNextNotifiedSessionNames());
    }

    @Test
    public void replyCaughtUpClearsTheSessionFromTracking() {
        PendingCallNotificationDecision decision =
            PendingCallNotificationDecision.decide(Collections.emptySet(), setOf("worker"));

        Assert.assertTrue(decision.getNewlyPendingSessionNames().isEmpty());
        Assert.assertTrue(decision.getNextNotifiedSessionNames().isEmpty());
    }

    @Test
    public void aSessionThatRepliedThenCallsAgainNotifiesAgain() {
        Set<String> trackedAfterFirstCall =
            PendingCallNotificationDecision.decide(setOf("worker"), Collections.emptySet())
                .getNextNotifiedSessionNames();

        Set<String> trackedAfterReply =
            PendingCallNotificationDecision.decide(Collections.emptySet(), trackedAfterFirstCall)
                .getNextNotifiedSessionNames();

        PendingCallNotificationDecision secondCall =
            PendingCallNotificationDecision.decide(setOf("worker"), trackedAfterReply);

        Assert.assertEquals(setOf("worker"), secondCall.getNewlyPendingSessionNames());
    }

    @Test
    public void onlyNewlyPendingSessionsFireWhenSomeAreAlreadyNotified() {
        PendingCallNotificationDecision decision =
            PendingCallNotificationDecision.decide(setOf("alpha", "beta"), setOf("alpha"));

        Assert.assertEquals(setOf("beta"), decision.getNewlyPendingSessionNames());
        Assert.assertEquals(setOf("alpha", "beta"), decision.getNextNotifiedSessionNames());
    }
}
