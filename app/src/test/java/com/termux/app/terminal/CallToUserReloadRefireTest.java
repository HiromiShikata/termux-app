package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

/**
 * Regression tests for the load/reload sessions re-fire bug: reloading or loading sessions
 * re-scans each session's transcript with a fresh in-memory scanner, and a terminal column resize
 * or scrollback reflow re-renders an already-seen {@code <call-to-user>} reason with its interior
 * whitespace shifted. The persisted activity store must still recognize the reason as already
 * known so the call-to-user is not re-fired or re-timestamped for every session on reload.
 */
public class CallToUserReloadRefireTest {

    private static final String SESSION = "session-one";

    /**
     * Builds a controller that records into {@code store} at a fixed time, mimicking a fresh
     * in-memory scanner created on the load/reload path (its dedup state is empty, so it re-detects
     * any tag still present in the transcript).
     */
    private static CallToUserTagController reloadControllerInto(SessionNewActivityStore store,
                                                                long fixedTimeMillis) {
        return new CallToUserTagController((sessionKey, reason) ->
            store.recordExplicitCall(sessionKey, fixedTimeMillis, reason));
    }

    @Test
    public void reloadDoesNotReArmAfterTheUserAcknowledgedTheCall() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        String transcript = "running <call-to-user>needs approval</call-to-user>";
        reloadControllerInto(store, 1_000L).onSessionTextChanged(SESSION, transcript);
        store.recordUserInput(SESSION, 2_000L);
        Assert.assertEquals(SessionNewActivityTier.NONE, store.tierFor(SESSION));

        reloadControllerInto(store, 9_000L).onSessionTextChanged(SESSION, transcript);

        Assert.assertEquals(SessionNewActivityTier.NONE, store.tierFor(SESSION));
    }

    @Test
    public void reloadDoesNotReArmAfterAcknowledgeEvenWhenTheReasonReflows() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        reloadControllerInto(store, 1_000L).onSessionTextChanged(SESSION,
            "<call-to-user>please approve the deploy to production now</call-to-user>");
        store.recordUserInput(SESSION, 2_000L);
        Assert.assertEquals(SessionNewActivityTier.NONE, store.tierFor(SESSION));

        // Reload re-renders the same reason with a line wrap inserted by a column resize.
        reloadControllerInto(store, 9_000L).onSessionTextChanged(SESSION,
            "<call-to-user>please approve the deploy\nto production now</call-to-user>");

        Assert.assertEquals(SessionNewActivityTier.NONE, store.tierFor(SESSION));
    }

    @Test
    public void reloadDoesNotBumpAnExistingPendingCallTimestampToNow() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        String transcript = "<call-to-user>needs approval</call-to-user>";
        reloadControllerInto(store, 1_000L).onSessionTextChanged(SESSION, transcript);
        Assert.assertEquals(SessionNewActivityTier.RED, store.tierFor(SESSION));
        Long firstDetectionTime = store.getLastExplicitCallTimeMillis(SESSION);

        reloadControllerInto(store, 9_000L).onSessionTextChanged(SESSION, transcript);

        Assert.assertEquals(SessionNewActivityTier.RED, store.tierFor(SESSION));
        Assert.assertEquals(firstDetectionTime, store.getLastExplicitCallTimeMillis(SESSION));
        Assert.assertEquals(Long.valueOf(1_000L), store.getLastExplicitCallTimeMillis(SESSION));
    }

    @Test
    public void reloadDoesNotBumpAnExistingPendingCallTimestampEvenWhenTheReasonReflows() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        reloadControllerInto(store, 1_000L).onSessionTextChanged(SESSION,
            "<call-to-user>please approve the deploy to production now</call-to-user>");
        Assert.assertEquals(SessionNewActivityTier.RED, store.tierFor(SESSION));

        reloadControllerInto(store, 9_000L).onSessionTextChanged(SESSION,
            "<call-to-user>please approve the deploy\nto production now</call-to-user>");

        Assert.assertEquals(Long.valueOf(1_000L), store.getLastExplicitCallTimeMillis(SESSION));
    }

    @Test
    public void reloadAcrossPersistenceRestartKeepsAcknowledgedCallsCleared() {
        InMemorySessionNewActivityPersistence persistence = new InMemorySessionNewActivityPersistence();
        SessionNewActivityStore beforeReload = new SessionNewActivityStore(persistence);
        reloadControllerInto(beforeReload, 1_000L).onSessionTextChanged(SESSION,
            "<call-to-user>needs approval</call-to-user>");
        beforeReload.recordUserInput(SESSION, 2_000L);
        Assert.assertEquals(SessionNewActivityTier.NONE, beforeReload.tierFor(SESSION));

        // Process restart: the store is rebuilt from persistence and the scanner state is fresh.
        SessionNewActivityStore afterReload = new SessionNewActivityStore(persistence);
        reloadControllerInto(afterReload, 9_000L).onSessionTextChanged(SESSION,
            "<call-to-user>needs approval</call-to-user>");

        Assert.assertEquals(SessionNewActivityTier.NONE, afterReload.tierFor(SESSION));
        Assert.assertEquals(Long.valueOf(1_000L), afterReload.getLastExplicitCallTimeMillis(SESSION));
    }

    @Test
    public void loadingManySessionsDoesNotMassFireCallToUserForAllOfThem() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        int sessionCount = 5;

        // Initial firing: each session has a distinct call-to-user that the user then acknowledges.
        for (int i = 0; i < sessionCount; i++) {
            String session = "session-" + i;
            reloadControllerInto(store, 1_000L).onSessionTextChanged(session,
                "<call-to-user>needs approval " + i + "</call-to-user>");
            store.recordUserInput(session, 2_000L);
            Assert.assertEquals(SessionNewActivityTier.NONE, store.tierFor(session));
        }

        // Loading sessions re-scans every transcript with a fresh scanner; none must re-arm.
        for (int i = 0; i < sessionCount; i++) {
            String session = "session-" + i;
            reloadControllerInto(store, 9_000L).onSessionTextChanged(session,
                "<call-to-user>needs approval " + i + "</call-to-user>");
        }

        for (int i = 0; i < sessionCount; i++) {
            String session = "session-" + i;
            Assert.assertEquals("session " + i + " must stay cleared after load",
                SessionNewActivityTier.NONE, store.tierFor(session));
        }
    }

    @Test
    public void aGenuinelyNewCallStillFiresAfterReload() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        reloadControllerInto(store, 1_000L).onSessionTextChanged(SESSION,
            "<call-to-user>first reason</call-to-user>");
        store.recordUserInput(SESSION, 2_000L);
        Assert.assertEquals(SessionNewActivityTier.NONE, store.tierFor(SESSION));

        // After reload a brand-new reason appears in the same session and MUST fire.
        reloadControllerInto(store, 9_000L).onSessionTextChanged(SESSION,
            "<call-to-user>first reason</call-to-user>\n<call-to-user>second reason</call-to-user>");

        Assert.assertEquals(SessionNewActivityTier.RED, store.tierFor(SESSION));
        Assert.assertEquals(Long.valueOf(9_000L), store.getLastExplicitCallTimeMillis(SESSION));
    }
}
