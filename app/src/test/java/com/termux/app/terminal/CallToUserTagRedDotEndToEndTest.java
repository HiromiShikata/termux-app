package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class CallToUserTagRedDotEndToEndTest {

    private static final String CALLED_SESSION = "called-session";
    private static final String OTHER_SESSION = "other-session";

    private static CallToUserTagController controllerRecordingInto(SessionNewActivityStore store) {
        return new CallToUserTagController((sessionKey, triggerValue, reason) ->
            store.recordExplicitCall(sessionKey, System.currentTimeMillis(), triggerValue, reason));
    }

    @Test
    public void callToUserTagProducesRedTierWithReason() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        CallToUserTagController controller = controllerRecordingInto(store);

        controller.onSessionTextChanged(CALLED_SESSION,
            "running <call-to-user>needs approval</call-to-user>");

        Assert.assertEquals(SessionNewActivityTier.RED, store.tierFor(CALLED_SESSION));
        Assert.assertEquals("needs approval", store.getLastExplicitCallReason(CALLED_SESSION));
    }

    @Test
    public void callToUserTagProducesRedTierWithJapaneseReason() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        CallToUserTagController controller = controllerRecordingInto(store);

        controller.onSessionTextChanged(CALLED_SESSION,
            "<call-to-user>承認をお願いします</call-to-user>");

        Assert.assertEquals(SessionNewActivityTier.RED, store.tierFor(CALLED_SESSION));
        Assert.assertEquals("承認をお願いします", store.getLastExplicitCallReason(CALLED_SESSION));
    }

    @Test
    public void redDotOnABackgroundCalledSessionSurvivesViewingAndClearsOnlyWhenTheUserReplies() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        CallToUserTagController controller = controllerRecordingInto(store);

        controller.onSessionTextChanged(CALLED_SESSION,
            "<call-to-user>needs approval</call-to-user>");

        long nowMillis = System.currentTimeMillis();
        for (long tick = nowMillis + 1_000L; tick <= nowMillis + 30_000L; tick += 1_000L) {
            store.recordSeen(OTHER_SESSION, tick);
        }

        Assert.assertEquals(SessionNewActivityTier.RED, store.tierFor(CALLED_SESSION));

        store.recordSeen(CALLED_SESSION, nowMillis + 31_000L);

        Assert.assertEquals(SessionNewActivityTier.RED, store.tierFor(CALLED_SESSION));

        store.recordUserInput(CALLED_SESSION, nowMillis + 32_000L);

        Assert.assertEquals(SessionNewActivityTier.NONE, store.tierFor(CALLED_SESSION));
    }

    @Test
    public void redDotOnTheActivelyViewedCalledSessionPersistsThroughSeenTicksUntilTheUserReplies() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        CallToUserTagController controller = controllerRecordingInto(store);

        controller.onSessionTextChanged(CALLED_SESSION,
            "<call-to-user>needs approval</call-to-user>");

        Assert.assertEquals(SessionNewActivityTier.RED, store.tierFor(CALLED_SESSION));

        long nowMillis = System.currentTimeMillis();
        store.recordSeen(CALLED_SESSION, nowMillis + 1_000L);

        Assert.assertEquals(SessionNewActivityTier.RED, store.tierFor(CALLED_SESSION));

        store.recordUserInput(CALLED_SESSION, nowMillis + 2_000L);

        Assert.assertEquals(SessionNewActivityTier.NONE, store.tierFor(CALLED_SESSION));
    }

    @Test
    public void reScanningTheSameCallToUserAfterScannerStateLossKeepsTheFirstDetectionTime() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        String transcript = "running <call-to-user>needs approval</call-to-user>";

        controllerRecordingAtFixedTime(store, 1_000L).onSessionTextChanged(CALLED_SESSION, transcript);
        Long firstDetectionTime = store.getLastExplicitCallTimeMillis(CALLED_SESSION);

        controllerRecordingAtFixedTime(store, 50_000L).onSessionTextChanged(CALLED_SESSION, transcript);
        controllerRecordingAtFixedTime(store, 90_000L).onSessionTextChanged(CALLED_SESSION, transcript);

        Assert.assertEquals(firstDetectionTime, store.getLastExplicitCallTimeMillis(CALLED_SESSION));
    }

    @Test
    public void reScanningTheSameCallToUserAfterTheUserRepliedDoesNotReArmTheRedDot() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        String transcript = "running <call-to-user>needs approval</call-to-user>";

        controllerRecordingAtFixedTime(store, 1_000L).onSessionTextChanged(CALLED_SESSION, transcript);
        store.recordUserInput(CALLED_SESSION, 2_000L);
        Assert.assertEquals(SessionNewActivityTier.NONE, store.tierFor(CALLED_SESSION));

        controllerRecordingAtFixedTime(store, 90_000L).onSessionTextChanged(CALLED_SESSION, transcript);

        Assert.assertEquals(SessionNewActivityTier.NONE, store.tierFor(CALLED_SESSION));
    }

    private static CallToUserTagController controllerRecordingAtFixedTime(SessionNewActivityStore store,
                                                                          long fixedTimeMillis) {
        return new CallToUserTagController((sessionKey, triggerValue, reason) ->
            store.recordExplicitCall(sessionKey, fixedTimeMillis, triggerValue, reason));
    }

    @Test
    public void redDotIsScopedToTheCallingSession() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        CallToUserTagController controller = controllerRecordingInto(store);

        controller.onSessionTextChanged(CALLED_SESSION,
            "<call-to-user>needs approval</call-to-user>");
        controller.onSessionTextChanged(OTHER_SESSION, "ordinary terminal output");

        Assert.assertEquals(SessionNewActivityTier.RED, store.tierFor(CALLED_SESSION));
        Assert.assertEquals(SessionNewActivityTier.NONE, store.tierFor(OTHER_SESSION));
    }
}
