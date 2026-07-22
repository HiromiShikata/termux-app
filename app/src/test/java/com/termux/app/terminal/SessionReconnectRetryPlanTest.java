package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class SessionReconnectRetryPlanTest {

    private static final long TIMEOUT_MILLIS = 30_000L;
    private static final long[] BACKOFF_MILLIS = {5_000L, 15_000L};

    private SessionReconnectRetryPlan newPlan() {
        return new SessionReconnectRetryPlan(TIMEOUT_MILLIS, BACKOFF_MILLIS);
    }

    @Test
    public void exposesTheConfiguredTimeoutAndRetryCap() {
        SessionReconnectRetryPlan plan = newPlan();

        Assert.assertEquals(TIMEOUT_MILLIS, plan.getTimeoutMillis());
        Assert.assertEquals(2, plan.getMaxRetries());
    }

    @Test
    public void timeoutWhileNotReconnectingIsANoOpAndDoesNotFail() {
        SessionReconnectRetryPlan plan = newPlan();
        SessionNewActivityStore store = new SessionNewActivityStore();

        SessionReconnectTimeoutOutcome outcome = plan.onTimeout(store, "session-one");

        Assert.assertEquals(SessionReconnectTimeoutOutcome.Decision.NOT_RECONNECTING,
            outcome.getDecision());
        Assert.assertFalse(store.isReconnectFailed("session-one"));
    }

    @Test
    public void firstTimeoutRetriesWithTheFirstBackoffAndKeepsSpinning() {
        SessionReconnectRetryPlan plan = newPlan();
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.setReconnecting("session-one", 1_000L);

        SessionReconnectTimeoutOutcome outcome = plan.onTimeout(store, "session-one");

        Assert.assertEquals(SessionReconnectTimeoutOutcome.Decision.RETRY, outcome.getDecision());
        Assert.assertEquals(5_000L, outcome.getRetryBackoffMillis());
        Assert.assertTrue("the spinner keeps showing while a retry is pending",
            store.isReconnecting("session-one"));
        Assert.assertFalse(store.isReconnectFailed("session-one"));
        Assert.assertEquals(1, store.getReconnectRetryAttempt("session-one"));
    }

    @Test
    public void secondTimeoutRetriesWithTheSecondBackoff() {
        SessionReconnectRetryPlan plan = newPlan();
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.setReconnecting("session-one", 1_000L);

        plan.onTimeout(store, "session-one");
        SessionReconnectTimeoutOutcome outcome = plan.onTimeout(store, "session-one");

        Assert.assertEquals(SessionReconnectTimeoutOutcome.Decision.RETRY, outcome.getDecision());
        Assert.assertEquals(15_000L, outcome.getRetryBackoffMillis());
        Assert.assertEquals(2, store.getReconnectRetryAttempt("session-one"));
    }

    @Test
    public void boundedAutoRetryStopsAfterItsCapAndLandsOnFailedStoppingTheSpinner() {
        SessionReconnectRetryPlan plan = newPlan();
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.setReconnecting("session-one", 1_000L);

        Assert.assertEquals(SessionReconnectTimeoutOutcome.Decision.RETRY,
            plan.onTimeout(store, "session-one").getDecision());
        Assert.assertEquals(SessionReconnectTimeoutOutcome.Decision.RETRY,
            plan.onTimeout(store, "session-one").getDecision());
        SessionReconnectTimeoutOutcome finalOutcome = plan.onTimeout(store, "session-one");

        Assert.assertEquals(SessionReconnectTimeoutOutcome.Decision.FAILED, finalOutcome.getDecision());
        Assert.assertTrue("the row settles on the failed state", store.isReconnectFailed("session-one"));
        Assert.assertFalse("the spinner stops once the row failed",
            store.isReconnecting("session-one"));
    }

    @Test
    public void statuslineArrivingBeforeTheTimeoutPreventsAnyFailedState() {
        SessionReconnectRetryPlan plan = newPlan();
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.setReconnecting("session-one", 1_000L);

        store.clearReconnecting("session-one");
        SessionReconnectTimeoutOutcome outcome = plan.onTimeout(store, "session-one");

        Assert.assertEquals(SessionReconnectTimeoutOutcome.Decision.NOT_RECONNECTING,
            outcome.getDecision());
        Assert.assertFalse(store.isReconnectFailed("session-one"));
        Assert.assertFalse(store.isReconnecting("session-one"));
    }

    @Test
    public void isolatesRowsSoOneStuckSessionDoesNotAffectAnother() {
        SessionReconnectRetryPlan plan = newPlan();
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.setReconnecting("stuck", 1_000L);
        store.setReconnecting("healthy", 1_000L);

        plan.onTimeout(store, "stuck");
        plan.onTimeout(store, "stuck");
        plan.onTimeout(store, "stuck");
        store.clearReconnecting("healthy");

        Assert.assertTrue(store.isReconnectFailed("stuck"));
        Assert.assertFalse(store.isReconnectFailed("healthy"));
        Assert.assertFalse(store.isReconnecting("healthy"));
    }
}
