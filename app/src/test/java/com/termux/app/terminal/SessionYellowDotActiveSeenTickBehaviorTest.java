package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class SessionYellowDotActiveSeenTickBehaviorTest {

    private static final String AGENT_SESSION = "agent-session";

    @Test
    public void activeSessionWhereOnlyTheOneSecondSeenTickRecordsSeenStaysNone() {
        SessionNewActivityStore store = new SessionNewActivityStore();

        long activeSessionSeenTickIntervalMillis = 1_000L;
        long lastTickMillis = 0L;
        for (long nowMillis = 1_000L; nowMillis <= 60_000L; nowMillis += 250L) {
            store.recordOutputActivity(AGENT_SESSION, nowMillis);
            if (nowMillis - lastTickMillis >= activeSessionSeenTickIntervalMillis) {
                store.recordSeen(AGENT_SESSION, nowMillis);
                lastTickMillis = nowMillis;
                Assert.assertEquals(SessionNewActivityTier.NONE, store.tierFor(AGENT_SESSION));
            }
        }
    }

    @Test
    public void activeSessionSeenTickClearsYellowFromOutputThatArrivedBeforeTheTick() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordSeen(AGENT_SESSION, 1_000L);
        store.recordOutputActivity(AGENT_SESSION, 1_400L);

        Assert.assertEquals(SessionNewActivityTier.YELLOW, store.tierFor(AGENT_SESSION));

        store.recordSeen(AGENT_SESSION, 2_000L);

        Assert.assertEquals(SessionNewActivityTier.NONE, store.tierFor(AGENT_SESSION));
    }

    @Test
    public void backgroundSessionWithNewOutputSinceLastViewShowsYellow() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordSeen(AGENT_SESSION, 1_000L);
        store.recordOutputActivity(AGENT_SESSION, 2_000L);

        Assert.assertEquals(SessionNewActivityTier.YELLOW, store.tierFor(AGENT_SESSION));
    }

    @Test
    public void backgroundSessionWithoutNewOutputSinceLastViewShowsNone() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordOutputActivity(AGENT_SESSION, 2_000L);
        store.recordSeen(AGENT_SESSION, 3_000L);

        Assert.assertEquals(SessionNewActivityTier.NONE, store.tierFor(AGENT_SESSION));
    }

    @Test
    public void continuousBackgroundOutputAfterLastViewStaysYellowRegardlessOfAwayDuration() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordSeen(AGENT_SESSION, 1_000L);

        for (long nowMillis = 1_100L; nowMillis <= 120_000L; nowMillis += 200L) {
            store.recordOutputActivity(AGENT_SESSION, nowMillis);
            Assert.assertEquals(SessionNewActivityTier.YELLOW, store.tierFor(AGENT_SESSION));
        }
    }

    @Test
    public void switchingAwayFreshlyViewedSessionLeavesItNoneUntilNewOutputArrives() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordOutputActivity(AGENT_SESSION, 5_000L);
        store.recordSeen(AGENT_SESSION, 5_000L);

        Assert.assertEquals(SessionNewActivityTier.NONE, store.tierFor(AGENT_SESSION));

        store.recordOutputActivity(AGENT_SESSION, 6_000L);

        Assert.assertEquals(SessionNewActivityTier.YELLOW, store.tierFor(AGENT_SESSION));
    }
}
