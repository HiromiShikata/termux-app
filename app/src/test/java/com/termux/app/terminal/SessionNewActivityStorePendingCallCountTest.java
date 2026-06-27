package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class SessionNewActivityStorePendingCallCountTest {

    @Test
    public void countIsZeroWithoutAnyCall() {
        SessionNewActivityStore store = new SessionNewActivityStore();

        Assert.assertEquals(0, store.pendingCallToUserSessionCount());
    }

    @Test
    public void countEqualsNumberOfSessionsWithPendingReasons() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordExplicitCall("worker-one", 1_000L, "needs approval");
        store.recordExplicitCall("worker-two", 2_000L, "needs input");
        store.recordOutputActivity("worker-three", 3_000L);

        Assert.assertEquals(2, store.pendingCallToUserSessionCount());
    }

    @Test
    public void multipleReasonsOnOneSessionCountAsOneSession() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordExplicitCall("worker", 1_000L, "needs approval");
        store.recordExplicitCall("worker", 2_000L, "needs input");

        Assert.assertEquals(2, store.getUnacknowledgedCallReasons("worker").size());
        Assert.assertEquals(1, store.pendingCallToUserSessionCount());
    }

    @Test
    public void acknowledgingViaUserInputDropsTheSessionFromTheCount() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordExplicitCall("worker-one", 1_000L, "needs approval");
        store.recordExplicitCall("worker-two", 2_000L, "needs input");
        Assert.assertEquals(2, store.pendingCallToUserSessionCount());

        store.recordUserInput("worker-one", 5_000L);

        Assert.assertEquals(1, store.pendingCallToUserSessionCount());
    }

    @Test
    public void purgingASessionDropsItFromTheCount() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordExplicitCall("worker-one", 1_000L, "needs approval");
        store.recordExplicitCall("worker-two", 2_000L, "needs input");

        store.purgeSession("worker-one");

        Assert.assertEquals(1, store.pendingCallToUserSessionCount());
    }

    @Test
    public void pruningToLiveSessionsDropsRemovedSessionsFromTheCount() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordExplicitCall("alive", 1_000L, "needs approval");
        store.recordExplicitCall("gone", 2_000L, "needs input");

        store.pruneToSessionNames(new HashSet<>(java.util.Collections.singletonList("alive")));

        Assert.assertEquals(1, store.pendingCallToUserSessionCount());
    }

    @Test
    public void countReturnsToZeroWhenLastPendingCallIsAcknowledged() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordExplicitCall("worker", 1_000L, "needs approval");
        Assert.assertEquals(1, store.pendingCallToUserSessionCount());

        store.recordUserInput("worker", 5_000L);

        Assert.assertEquals(0, store.pendingCallToUserSessionCount());
    }

    @Test
    public void changeListenerFiresWithUpdatedCountOnAddAndAcknowledgeAndPurge() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        List<Integer> observedCounts = new ArrayList<>();
        store.setOnChangeListener(changed ->
            observedCounts.add(changed.pendingCallToUserSessionCount()));

        store.recordExplicitCall("worker-one", 1_000L, "needs approval");
        store.recordExplicitCall("worker-two", 2_000L, "needs input");
        store.recordUserInput("worker-one", 5_000L);
        store.purgeSession("worker-two");

        Assert.assertEquals(1, observedCounts.get(0).intValue());
        Assert.assertEquals(2, observedCounts.get(1).intValue());
        Assert.assertEquals(1, observedCounts.get(2).intValue());
        Assert.assertEquals(0, observedCounts.get(observedCounts.size() - 1).intValue());
    }
}
