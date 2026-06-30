package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class SessionReconnectingIndicatorStateTest {

    @Test
    public void notReconnectingDoesNotShowIndicator() {
        SessionNewActivityStore store = new SessionNewActivityStore();

        Assert.assertFalse(SessionReconnectingIndicatorState.shouldShowReconnectingIndicator(
            "session-one", store));
    }

    @Test
    public void reconnectingShowsIndicator() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.setReconnecting("session-one", 10_000L);

        Assert.assertTrue(SessionReconnectingIndicatorState.shouldShowReconnectingIndicator(
            "session-one", store));
    }

    @Test
    public void reconnectingNeverTimesOutOnAClock() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.setReconnecting("session-one", 10_000L);

        Assert.assertTrue(SessionReconnectingIndicatorState.shouldShowReconnectingIndicator(
            "session-one", store));
    }

    @Test
    public void clearingReconnectingHidesIndicator() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.setReconnecting("session-one", 10_000L);
        store.clearReconnecting("session-one");

        Assert.assertFalse(SessionReconnectingIndicatorState.shouldShowReconnectingIndicator(
            "session-one", store));
    }

    @Test
    public void arrivingStatuslineDataClearsTheReconnectingIndicator() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.setReconnecting("session-one", 10_000L);

        store.clearReconnecting("session-one");
        store.recordStatuslineTimes("session-one", null, 20_000L, null);

        Assert.assertFalse(SessionReconnectingIndicatorState.shouldShowReconnectingIndicator(
            "session-one", store));
    }

    @Test
    public void staleSessionWithNoActiveLoadShowsNoIndicator() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordStatuslineTimes("session-one", null, 1_000L, null);

        Assert.assertFalse(SessionReconnectingIndicatorState.shouldShowReconnectingIndicator(
            "session-one", store));
    }

    @Test
    public void nullStoreDoesNotShowIndicator() {
        Assert.assertFalse(SessionReconnectingIndicatorState.shouldShowReconnectingIndicator(
            "session-one", null));
    }
}
