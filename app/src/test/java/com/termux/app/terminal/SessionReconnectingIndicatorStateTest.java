package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class SessionReconnectingIndicatorStateTest {

    @Test
    public void notReconnectingDoesNotShowIndicator() {
        SessionNewActivityStore store = new SessionNewActivityStore();

        Assert.assertFalse(SessionReconnectingIndicatorState.shouldShowReconnectingIndicator(
            "session-one", store, 10_000L));
    }

    @Test
    public void reconnectingWithinCapShowsIndicator() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.setReconnecting("session-one", 10_000L);

        Assert.assertTrue(SessionReconnectingIndicatorState.shouldShowReconnectingIndicator(
            "session-one", store, 10_000L
                + SessionReconnectingIndicatorState.RECONNECTING_INDICATOR_MAX_DURATION_MILLIS));
    }

    @Test
    public void reconnectingPastCapTimesOutAndHidesIndicator() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.setReconnecting("session-one", 10_000L);

        Assert.assertFalse(SessionReconnectingIndicatorState.shouldShowReconnectingIndicator(
            "session-one", store, 10_000L
                + SessionReconnectingIndicatorState.RECONNECTING_INDICATOR_MAX_DURATION_MILLIS + 1L));
    }

    @Test
    public void nullStoreDoesNotShowIndicator() {
        Assert.assertFalse(SessionReconnectingIndicatorState.shouldShowReconnectingIndicator(
            "session-one", null, 10_000L));
    }
}
