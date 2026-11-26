package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

public class SessionNewActivityStoreCallToUserAlertTest {

    @Test
    public void newCallWithReasonIsReportedAsRecordedAndSameCycleKeyIsNot() {
        SessionNewActivityStore store = new SessionNewActivityStore();

        Assert.assertTrue(store.recordExplicitCall("worker", 1_000L, "needs approval", "cycle-1"));
        Assert.assertFalse(store.recordExplicitCall("worker", 2_000L, "needs approval", "cycle-1"));
        Assert.assertTrue(store.recordExplicitCall("worker", 3_000L, "needs approval", "cycle-2"));
    }

    @Test
    public void callWithBlankReasonIsNotReportedAsRecorded() {
        SessionNewActivityStore store = new SessionNewActivityStore();

        Assert.assertFalse(store.recordExplicitCall("worker", 1_000L, "   ", "cycle-1"));
        Assert.assertEquals(Collections.emptySet(), store.pendingCallToUserSessionNames());
    }

    @Test
    public void pendingSessionNamesListOnlySessionsWithAnUnansweredCall() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordExplicitCall("worker", 1_000L, "needs approval", "cycle-1");
        store.recordExplicitCall("reviewer", 1_500L, "needs a look", "cycle-2");

        Assert.assertEquals(new HashSet<>(Arrays.asList("worker", "reviewer")),
            store.pendingCallToUserSessionNames());
    }

    @Test
    public void appReplyClearsThatSessionFromThePendingSessionNames() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordExplicitCall("worker", 1_000L, "needs approval", "cycle-1");
        store.recordExplicitCall("reviewer", 1_500L, "needs a look", "cycle-2");

        store.recordGenuineAppReply("worker", 2_000L);

        Assert.assertEquals(new HashSet<>(Collections.singletonList("reviewer")),
            store.pendingCallToUserSessionNames());
    }
}
