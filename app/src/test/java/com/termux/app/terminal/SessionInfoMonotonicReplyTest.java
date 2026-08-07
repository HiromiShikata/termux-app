package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class SessionInfoMonotonicReplyTest {

    private static final long ONE_SECOND_MILLIS = 1000L;
    private static final long ONE_MINUTE_MILLIS = 60L * ONE_SECOND_MILLIS;
    private static final long NOW = 1_000_000_000L;
    private static final String CURRENT_SESSION = "current-session";

    @Test
    public void olderAppInputDoesNotLowerTheStoredUserInputTime() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordUserInput(CURRENT_SESSION, NOW);

        store.recordUserInput(CURRENT_SESSION, NOW - 51L * ONE_SECOND_MILLIS);

        Assert.assertEquals(Long.valueOf(NOW), store.getLastUserInputTimeMillis(CURRENT_SESSION));
    }

    @Test
    public void newerAppInputAdvancesTheStoredUserInputTime() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordUserInput(CURRENT_SESSION, NOW - 51L * ONE_SECOND_MILLIS);

        store.recordUserInput(CURRENT_SESSION, NOW);

        Assert.assertEquals(Long.valueOf(NOW), store.getLastUserInputTimeMillis(CURRENT_SESSION));
    }

    @Test
    public void recordStatuslineTimesDoesNotPullTheAppInputTimeBackwards() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordUserInput(CURRENT_SESSION, NOW);

        store.recordStatuslineTimes(CURRENT_SESSION, null, null, NOW - 51L * ONE_SECOND_MILLIS);

        Assert.assertEquals("recordStatuslineTimes must never write the statusline reply into the "
                + "app-captured input time", Long.valueOf(NOW),
            store.getLastUserInputTimeMillis(CURRENT_SESSION));
    }

    @Test
    public void olderStatuslineReplyDoesNotLowerTheEffectiveReplyTime() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordUserInput(CURRENT_SESSION, NOW);

        store.recordStatuslineTimes(CURRENT_SESSION, null, null, NOW - 51L * ONE_SECOND_MILLIS);

        Assert.assertEquals("an older statusline reply must not lower the effective reply time",
            Long.valueOf(NOW), store.effectiveReplyTimeMillis(CURRENT_SESSION));
    }

    @Test
    public void aNewerStatuslineReplyAdvancesTheEffectiveReplyTime() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordUserInput(CURRENT_SESSION, NOW - 51L * ONE_SECOND_MILLIS);

        store.recordStatuslineTimes(CURRENT_SESSION, null, null, NOW);

        Assert.assertEquals("a newer reply must advance the effective reply time",
            Long.valueOf(NOW), store.effectiveReplyTimeMillis(CURRENT_SESSION));
    }

    @Test
    public void anOlderStatuslineReplyDoesNotLowerAPreviousStatuslineReply() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordStatuslineTimes(CURRENT_SESSION, null, null, NOW);

        store.recordStatuslineTimes(CURRENT_SESSION, null, null, NOW - 2L * ONE_MINUTE_MILLIS);

        Assert.assertEquals("an older statusline reply must not lower the stored statusline reply",
            Long.valueOf(NOW), store.getStatuslineReplyTimeMillis(CURRENT_SESSION));
    }
}
