package com.termux.app.terminal;

import com.termux.app.terminal.session.SessionNewActivityStateCaps;

import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

public class SessionNewActivityStoreReasonCapTest {

    private static final String SESSION = "session-one";

    @Test
    public void recordExplicitCallTruncatesOversizedReason() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        StringBuilder oversizedReason = new StringBuilder();
        for (int index = 0; index < SessionNewActivityStateCaps.MAX_REASON_LENGTH + 1_000; index++) {
            oversizedReason.append('x');
        }

        store.recordExplicitCall(SESSION, 1_000L, oversizedReason.toString());

        Assert.assertEquals(SessionNewActivityStateCaps.MAX_REASON_LENGTH,
            store.getLastExplicitCallReason(SESSION).length());
        List<String> reasons = store.getUnacknowledgedCallReasons(SESSION);
        Assert.assertEquals(1, reasons.size());
        Assert.assertEquals(SessionNewActivityStateCaps.MAX_REASON_LENGTH, reasons.get(0).length());
    }

    @Test
    public void recordExplicitCallCapsUnacknowledgedListToLastN() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        int total = SessionNewActivityStateCaps.MAX_REASONS_PER_SESSION + 15;
        for (int index = 0; index < total; index++) {
            store.recordExplicitCall(SESSION, 1_000L + index, "reason-" + index);
        }

        List<String> reasons = store.getUnacknowledgedCallReasons(SESSION);
        Assert.assertEquals(SessionNewActivityStateCaps.MAX_REASONS_PER_SESSION, reasons.size());
        Assert.assertEquals("reason-" + (total - 1), reasons.get(reasons.size() - 1));
    }

    @Test
    public void cappedReasonStillDrivesRedTierAndSceneContent() {
        SessionNewActivityStore store = new SessionNewActivityStore();

        store.recordExplicitCall(SESSION, 1_000L, "please approve the deploy");

        Assert.assertEquals(SessionNewActivityTier.RED, store.tierFor(SESSION));
        Assert.assertEquals(Collections.singletonList("please approve the deploy"),
            store.getUnacknowledgedCallReasons(SESSION));
        Assert.assertEquals("please approve the deploy", store.getLastExplicitCallReason(SESSION));
    }

    @Test
    public void userInputClearsCappedReasonsAndDropsOutOfRed() {
        SessionNewActivityStore store = new SessionNewActivityStore();
        store.recordExplicitCall(SESSION, 1_000L, "please approve the deploy");

        store.recordUserInput(SESSION, 2_000L);

        Assert.assertEquals(SessionNewActivityTier.NONE, store.tierFor(SESSION));
        Assert.assertTrue(store.getUnacknowledgedCallReasons(SESSION).isEmpty());
    }
}
