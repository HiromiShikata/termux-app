package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class CallToUserTagRedDotEndToEndTest {

    private static final String CALLED_SESSION = "called-session";
    private static final String OTHER_SESSION = "other-session";

    private static CallToUserTagController controllerRecordingInto(SessionNewActivityStore store) {
        return new CallToUserTagController((sessionKey, reason) ->
            store.recordExplicitCall(sessionKey, System.currentTimeMillis(), reason));
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
    public void redDotOnABackgroundCalledSessionSurvivesUntilThatSessionIsViewed() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        CallToUserTagController controller = controllerRecordingInto(store);

        controller.onSessionTextChanged(CALLED_SESSION,
            "<call-to-user>needs approval</call-to-user>");

        // While the called session is in the background, only the active session's seen
        // is ticked; the called session's seen is never recorded, so its red dot stays.
        long nowMillis = System.currentTimeMillis();
        for (long tick = nowMillis + 1_000L; tick <= nowMillis + 30_000L; tick += 1_000L) {
            store.recordSeen(OTHER_SESSION, tick);
        }

        Assert.assertEquals(SessionNewActivityTier.RED, store.tierFor(CALLED_SESSION));

        // Viewing the called session records its seen and clears the red dot.
        store.recordSeen(CALLED_SESSION, nowMillis + 31_000L);

        Assert.assertEquals(SessionNewActivityTier.NONE, store.tierFor(CALLED_SESSION));
    }

    @Test
    public void redDotOnTheActivelyViewedCalledSessionClearsOnTheNextSeenTick() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        CallToUserTagController controller = controllerRecordingInto(store);

        controller.onSessionTextChanged(CALLED_SESSION,
            "<call-to-user>needs approval</call-to-user>");

        Assert.assertEquals(SessionNewActivityTier.RED, store.tierFor(CALLED_SESSION));

        // The actively viewed session's per-second seen tick records seen unconditionally,
        // so the red dot clears within about a second without a manual switch.
        long nowMillis = System.currentTimeMillis();
        store.recordSeen(CALLED_SESSION, nowMillis + 1_000L);

        Assert.assertEquals(SessionNewActivityTier.NONE, store.tierFor(CALLED_SESSION));
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
