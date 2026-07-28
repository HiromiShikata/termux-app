package com.termux.app.terminal.session;

import com.termux.app.terminal.SessionNewActivityState;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SessionNewActivityStateCapsTest {

    @Test
    public void capReasonTruncatesToMaxLength() {
        String oversized = repeat("x", SessionNewActivityStateCaps.MAX_REASON_LENGTH + 500);

        String capped = SessionNewActivityStateCaps.capReason(oversized);

        Assert.assertEquals(SessionNewActivityStateCaps.MAX_REASON_LENGTH, capped.length());
    }

    @Test
    public void capReasonKeepsShortReasonUnchanged() {
        Assert.assertEquals("needs approval", SessionNewActivityStateCaps.capReason("needs approval"));
    }

    @Test
    public void capReasonsKeepsOnlyLastN() {
        List<String> reasons = new ArrayList<>();
        for (int index = 0; index < SessionNewActivityStateCaps.MAX_REASONS_PER_SESSION + 7; index++) {
            reasons.add("reason-" + index);
        }

        List<String> capped = SessionNewActivityStateCaps.capReasons(reasons);

        Assert.assertEquals(SessionNewActivityStateCaps.MAX_REASONS_PER_SESSION, capped.size());
        Assert.assertEquals("reason-" + (reasons.size() - 1), capped.get(capped.size() - 1));
        Assert.assertEquals("reason-7", capped.get(0));
    }

    @Test
    public void capReasonsTruncatesEachReason() {
        String oversized = repeat("y", SessionNewActivityStateCaps.MAX_REASON_LENGTH + 100);

        List<String> capped = SessionNewActivityStateCaps.capReasons(Arrays.asList(oversized));

        Assert.assertEquals(SessionNewActivityStateCaps.MAX_REASON_LENGTH, capped.get(0).length());
    }

    @Test
    public void capStateCapsExplicitReasonAndBothLists() {
        String oversized = repeat("z", SessionNewActivityStateCaps.MAX_REASON_LENGTH + 50);
        List<String> manyUnacknowledged = new ArrayList<>();
        List<String> manyAcknowledged = new ArrayList<>();
        for (int index = 0; index < SessionNewActivityStateCaps.MAX_REASONS_PER_SESSION + 5; index++) {
            manyUnacknowledged.add("u-" + index);
            manyAcknowledged.add("a-" + index);
        }
        SessionNewActivityState state = new SessionNewActivityState("session-one", 1L, 2L, oversized,
            3L, 4L, manyUnacknowledged, manyAcknowledged, 5L, 6L, 7L);

        SessionNewActivityState capped = SessionNewActivityStateCaps.capState(state);

        Assert.assertEquals(SessionNewActivityStateCaps.MAX_REASON_LENGTH,
            capped.getLastExplicitCallReason().length());
        Assert.assertEquals(SessionNewActivityStateCaps.MAX_REASONS_PER_SESSION,
            capped.getUnacknowledgedCallReasons().size());
        Assert.assertEquals(SessionNewActivityStateCaps.MAX_REASONS_PER_SESSION,
            capped.getCallTriggerValues().size());
        Assert.assertEquals(Long.valueOf(5L), capped.getStatuslineCallTimeMillis());
    }

    @Test
    public void capStateLeavesNullReasonsNull() {
        SessionNewActivityState state =
            new SessionNewActivityState("session-one", 1L, null, null, null, null);

        SessionNewActivityState capped = SessionNewActivityStateCaps.capState(state);

        Assert.assertNull(capped.getLastExplicitCallReason());
        Assert.assertNull(capped.getUnacknowledgedCallReasons());
        Assert.assertNull(capped.getCallTriggerValues());
    }

    private static String repeat(String unit, int count) {
        StringBuilder builder = new StringBuilder(unit.length() * count);
        for (int index = 0; index < count; index++) {
            builder.append(unit);
        }
        return builder.toString();
    }
}
