package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.HashSet;

public class SessionNewActivityStorePendingCallNamesTest {

    @Test
    public void emptyWithoutAnyCall() {
        SessionNewActivityStore store = new SessionNewActivityStore();

        Assert.assertTrue(store.pendingCallToUserSessionNames().isEmpty());
    }

    @Test
    public void recordedReasonMakesSessionPending() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordExplicitCall("worker", 1_000L, "needs approval");

        Assert.assertEquals(new HashSet<>(Collections.singletonList("worker")),
            store.pendingCallToUserSessionNames());
    }

    @Test
    public void statuslineCallNewerThanReplyMakesSessionPendingWithoutRecordedReason() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordStatuslineTimes("claude-worker", 5_000L, 5_000L, 1_000L, 0);

        Assert.assertTrue(store.getUnacknowledgedCallReasons("claude-worker").isEmpty());
        Assert.assertEquals(new HashSet<>(Collections.singletonList("claude-worker")),
            store.pendingCallToUserSessionNames());
    }

    @Test
    public void statuslineReplyCaughtUpToCallClearsPending() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordStatuslineTimes("claude-worker", 5_000L, 5_000L, 1_000L, 0);
        Assert.assertEquals(1, store.pendingCallToUserSessionNames().size());

        store.recordStatuslineTimes("claude-worker", 5_000L, 6_000L, 6_000L, 0);

        Assert.assertTrue(store.pendingCallToUserSessionNames().isEmpty());
    }

    @Test
    public void userInputClearsRecordedReasonPending() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordExplicitCall("worker", 1_000L, "needs approval");
        Assert.assertEquals(1, store.pendingCallToUserSessionNames().size());

        store.recordUserInput("worker", 5_000L);

        Assert.assertTrue(store.pendingCallToUserSessionNames().isEmpty());
    }

    @Test
    public void multiplePendingSessionsAreAllReported() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordExplicitCall("reason-worker", 1_000L, "needs approval");
        store.recordStatuslineTimes("statusline-worker", 5_000L, 5_000L, 1_000L, 0);
        store.recordOutputActivity("idle-worker", 2_000L);

        Assert.assertEquals(new HashSet<>(java.util.Arrays.asList("reason-worker", "statusline-worker")),
            store.pendingCallToUserSessionNames());
    }

    @Test
    public void countMatchesNamesSize() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordExplicitCall("reason-worker", 1_000L, "needs approval");
        store.recordStatuslineTimes("statusline-worker", 5_000L, 5_000L, 1_000L, 0);

        Assert.assertEquals(store.pendingCallToUserSessionNames().size(),
            store.pendingCallToUserSessionCount());
    }
}
