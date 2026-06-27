package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class CurrentPendingCallToUserSceneFreshnessTest {

    private static final String SESSION = "worker-session";
    private static final long ONE_MINUTE_MILLIS = 60L * 1000L;

    @Test
    public void aReasonRecordedBeforeTheCurrentStatuslineCallIsNotShownAsTheCurrentScene() {
        SessionNewActivityStore store = new SessionNewActivityStore();

        long firstCallMillis = 12L * 60L * 60L * 1000L;
        store.recordExplicitCall(SESSION, firstCallMillis, "old cycle reason");

        long secondCallMillis = firstCallMillis + 10L * ONE_MINUTE_MILLIS;
        store.recordStatuslineTimes(SESSION, secondCallMillis, secondCallMillis, null);

        Assert.assertEquals(SessionNewActivityTier.RED, store.tierFor(SESSION));
        Assert.assertNull("a reason recorded before the current statusline call belongs to a previous "
                + "call-to-user cycle and must not be shown as the current pending scene",
            store.currentPendingCallToUserReason(SESSION));
    }

    @Test
    public void aReasonRecordedForTheCurrentStatuslineCallIsShownAsTheCurrentScene() {
        SessionNewActivityStore store = new SessionNewActivityStore();

        long callMillis = 12L * 60L * 60L * 1000L;
        store.recordStatuslineTimes(SESSION, callMillis, callMillis, null);
        store.recordExplicitCall(SESSION, callMillis, "current cycle reason");

        Assert.assertEquals(SessionNewActivityTier.RED, store.tierFor(SESSION));
        Assert.assertEquals("a reason recorded at the current statusline call time is the current "
                + "scene", "current cycle reason", store.currentPendingCallToUserReason(SESSION));
    }

    @Test
    public void aNewerReasonReplacesTheDisplayedSceneForTheSameSession() {
        SessionNewActivityStore store = new SessionNewActivityStore();

        long firstCallMillis = 12L * 60L * 60L * 1000L;
        store.recordExplicitCall(SESSION, firstCallMillis, "earlier reason");
        store.recordExplicitCall(SESSION, firstCallMillis + ONE_MINUTE_MILLIS, "latest reason");

        Assert.assertEquals("the displayed scene must reflect the latest call-to-user content",
            "latest reason", store.currentPendingCallToUserReason(SESSION));
    }

    @Test
    public void anAcknowledgedSessionShowsNoScene() {
        SessionNewActivityStore store = new SessionNewActivityStore();

        long callMillis = 12L * 60L * 60L * 1000L;
        store.recordExplicitCall(SESSION, callMillis, "needs approval");
        store.recordUserInput(SESSION, callMillis + ONE_MINUTE_MILLIS);

        Assert.assertNotEquals(SessionNewActivityTier.RED, store.tierFor(SESSION));
        Assert.assertNull("after the owner replies the scene must show nothing, not the acknowledged "
            + "reason", store.currentPendingCallToUserReason(SESSION));
    }

    @Test
    public void aReasonArmedSessionWithNoStatuslineShowsItsReason() {
        SessionNewActivityStore store = new SessionNewActivityStore();

        long callMillis = 12L * 60L * 60L * 1000L;
        store.recordExplicitCall(SESSION, callMillis, "non-claude session reason");

        Assert.assertEquals(SessionNewActivityTier.RED, store.tierFor(SESSION));
        Assert.assertEquals("a session that has only the tag-scan signal and no statusline must show "
                + "its unacknowledged reason", "non-claude session reason",
            store.currentPendingCallToUserReason(SESSION));
    }
}
