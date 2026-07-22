package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class SessionNewActivityStoreReconnectFailedStateTest {

    @Test
    public void failingClearsReconnectingSoTheSpinnerStops() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.setReconnecting("session-one", 1_000L);

        store.setReconnectFailed("session-one");

        Assert.assertFalse(store.isReconnecting("session-one"));
        Assert.assertTrue(store.isReconnectFailed("session-one"));
    }

    @Test
    public void armingAFreshReconnectClearsTheFailedState() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.setReconnectFailed("session-one");

        store.setReconnecting("session-one", 2_000L);

        Assert.assertTrue(store.isReconnecting("session-one"));
        Assert.assertFalse(store.isReconnectFailed("session-one"));
    }

    @Test
    public void statuslineArrivalClearsReconnectingFailedAndTheRetryCounter() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.setReconnecting("session-one", 1_000L);
        store.incrementReconnectRetryAttempt("session-one");
        store.setReconnectFailed("session-one");

        store.clearReconnecting("session-one");

        Assert.assertFalse(store.isReconnecting("session-one"));
        Assert.assertFalse(store.isReconnectFailed("session-one"));
        Assert.assertEquals(0, store.getReconnectRetryAttempt("session-one"));
    }

    @Test
    public void tappingAFailedRowReArmsReconnectingWithAFreshRetryBudget() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.setReconnecting("session-one", 1_000L);
        store.incrementReconnectRetryAttempt("session-one");
        store.incrementReconnectRetryAttempt("session-one");
        store.setReconnectFailed("session-one");

        store.clearReconnectFailed("session-one");
        store.resetReconnectRetryAttempt("session-one");
        store.setReconnecting("session-one", 5_000L);

        Assert.assertTrue(store.isReconnecting("session-one"));
        Assert.assertFalse(store.isReconnectFailed("session-one"));
        Assert.assertEquals(0, store.getReconnectRetryAttempt("session-one"));
    }

    @Test
    public void retryAttemptIncrementsAndResets() {
        SessionNewActivityStore store = new SessionNewActivityStore();

        Assert.assertEquals(0, store.getReconnectRetryAttempt("session-one"));
        Assert.assertEquals(1, store.incrementReconnectRetryAttempt("session-one"));
        Assert.assertEquals(2, store.incrementReconnectRetryAttempt("session-one"));

        store.resetReconnectRetryAttempt("session-one");

        Assert.assertEquals(0, store.getReconnectRetryAttempt("session-one"));
    }

    @Test
    public void purgingASessionClearsItsFailedStateAndRetryCounter() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.setReconnectFailed("session-one");
        store.incrementReconnectRetryAttempt("session-one");

        store.purgeSession("session-one");

        Assert.assertFalse(store.isReconnectFailed("session-one"));
        Assert.assertEquals(0, store.getReconnectRetryAttempt("session-one"));
    }

    @Test
    public void pruningToKnownSessionsDropsFailedStateForRemovedSessions() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.setReconnectFailed("gone");
        store.incrementReconnectRetryAttempt("gone");
        store.setReconnectFailed("kept");

        store.pruneToSessionNames(java.util.Collections.singleton("kept"));

        Assert.assertFalse(store.isReconnectFailed("gone"));
        Assert.assertEquals(0, store.getReconnectRetryAttempt("gone"));
        Assert.assertTrue(store.isReconnectFailed("kept"));
    }
}
