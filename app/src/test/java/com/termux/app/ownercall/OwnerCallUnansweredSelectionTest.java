package com.termux.app.ownercall;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class OwnerCallUnansweredSelectionTest {

    private static final String SESSION_NAME = "termux-app-issues-1949";
    private static final long REPLIED_AT_EPOCH_MILLIS = 1786681348000L;
    private static final long NOW_EPOCH_MILLIS = REPLIED_AT_EPOCH_MILLIS + 60 * 60 * 1000L;

    private static OwnerCall callAt(String calledAt) {
        return new OwnerCall(SESSION_NAME, calledAt, "the owner is asked to decide");
    }

    private static final OwnerCall ANSWERED_BY_THE_REPLY = callAt("2026-08-14T04:20:00Z");
    private static final OwnerCall AT_THE_REPLY = callAt("2026-08-14T04:22:28Z");
    private static final OwnerCall STILL_WAITING = callAt("2026-08-14T04:25:00Z");

    @Test
    public void keepsOnlyTheCallsPlacedAfterTheOwnerRepliedToTheSession() {
        List<OwnerCall> unanswered = OwnerCallUnansweredSelection.of(
            Arrays.asList(ANSWERED_BY_THE_REPLY, AT_THE_REPLY, STILL_WAITING),
            REPLIED_AT_EPOCH_MILLIS, NOW_EPOCH_MILLIS);

        Assert.assertEquals(Collections.singletonList(STILL_WAITING), unanswered);
    }

    @Test
    public void keepsEveryCallWhileTheSessionHasNoReplyTimeYet() {
        List<OwnerCall> calls = Arrays.asList(ANSWERED_BY_THE_REPLY, STILL_WAITING);

        Assert.assertEquals(calls,
            OwnerCallUnansweredSelection.of(calls, null, NOW_EPOCH_MILLIS));
    }

    @Test
    public void keepsACallWhoseTimeCannotBeReadBecauseItIsNoEvidenceOfAnAnswer() {
        OwnerCall unreadableCallTime = callAt("yesterday");

        Assert.assertEquals(Collections.singletonList(unreadableCallTime),
            OwnerCallUnansweredSelection.of(Collections.singletonList(unreadableCallTime),
                REPLIED_AT_EPOCH_MILLIS, NOW_EPOCH_MILLIS));
    }

    @Test
    public void keepsNothingWhenTheReplyCameAfterEveryCall() {
        Assert.assertTrue(OwnerCallUnansweredSelection.of(
            Arrays.asList(ANSWERED_BY_THE_REPLY, AT_THE_REPLY),
            REPLIED_AT_EPOCH_MILLIS, NOW_EPOCH_MILLIS).isEmpty());
    }

    @Test
    public void keepsEveryCallWhenTheStoredReplyTimeLiesInTheFuture() {
        List<OwnerCall> calls = Arrays.asList(ANSWERED_BY_THE_REPLY, AT_THE_REPLY, STILL_WAITING);

        Assert.assertEquals(calls, OwnerCallUnansweredSelection.of(calls,
            NOW_EPOCH_MILLIS + 1, NOW_EPOCH_MILLIS));
    }
}
