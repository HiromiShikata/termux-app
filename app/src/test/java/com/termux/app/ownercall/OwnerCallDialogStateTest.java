package com.termux.app.ownercall;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class OwnerCallDialogStateTest {

    private static final String SESSION_URL =
        "https://github.com/HiromiShikata/termux-app/issues/1884";
    private static final String OTHER_SESSION_URL =
        "https://github.com/HiromiShikata/termux-app/issues/1885";

    private static OwnerCall call(String sessionName, String calledAt, String body) {
        return new OwnerCall(sessionName, calledAt, body);
    }

    private static final OwnerCall OLDEST_CALL =
        call(SESSION_URL, "2026-08-14T04:00:00Z", "Decide whether the addresses may be deleted.");
    private static final OwnerCall MIDDLE_CALL =
        call(SESSION_URL, "2026-08-14T04:03:00Z",
            "Decide whether the invoice recipient may be changed.");
    private static final OwnerCall NEWEST_CALL =
        call(SESSION_URL, "2026-08-14T04:05:00Z",
            "Decide whether the invoice recipient may be changed.");
    private static final List<OwnerCall> THREE_CALLS =
        Arrays.asList(OLDEST_CALL, MIDDLE_CALL, NEWEST_CALL);

    @Test
    public void startsAtTheNewestCallOfANewlyDisplayedSession() {
        OwnerCallDialogState state = new OwnerCallDialogState();
        state.displaySession(SESSION_URL);
        state.displayCallAt(THREE_CALLS, 2);

        state.displaySession(OTHER_SESSION_URL);

        Assert.assertEquals(2, state.indexOfDisplayedCall(THREE_CALLS));
    }

    @Test
    public void startsAtTheNewestCallHoweverManyCallsAreWaiting() {
        List<OwnerCall> manyCalls = new ArrayList<>();
        for (int minute = 0; minute < 4882; minute++) {
            manyCalls.add(call(SESSION_URL, String.format(Locale.ROOT, "2026-08-14T%02d:%02d:00Z",
                minute / 60, minute % 60), "Decide whether the addresses may be deleted."));
        }
        OwnerCallDialogState state = new OwnerCallDialogState();

        state.displaySession(SESSION_URL);

        Assert.assertEquals(manyCalls.size() - 1, state.indexOfDisplayedCall(manyCalls));
    }

    @Test
    public void returnsToTheNewestCallWhenTheCallBeingReadIsNoLongerWaiting() {
        OwnerCallDialogState state = new OwnerCallDialogState();
        state.displaySession(SESSION_URL);
        state.displayCallAt(THREE_CALLS, 0);

        Assert.assertEquals(1,
            state.indexOfDisplayedCall(Arrays.asList(MIDDLE_CALL, NEWEST_CALL)));
    }

    @Test
    public void staysAtTheFirstIndexWhileNoCallIsWaiting() {
        OwnerCallDialogState state = new OwnerCallDialogState();
        state.displaySession(SESSION_URL);

        Assert.assertEquals(0, state.indexOfDisplayedCall(Collections.emptyList()));
    }

    @Test
    public void keepsShowingTheCallBeingReadWhenAnEarlierCallDropsOutOfTheFile() {
        OwnerCallDialogState state = new OwnerCallDialogState();
        state.displaySession(SESSION_URL);
        state.displayCallAt(THREE_CALLS, 1);

        Assert.assertEquals(0,
            state.indexOfDisplayedCall(Arrays.asList(MIDDLE_CALL, NEWEST_CALL)));
    }

    @Test
    public void remembersWhichSessionIsDisplayed() {
        OwnerCallDialogState state = new OwnerCallDialogState();

        state.displaySession(SESSION_URL);

        Assert.assertEquals(SESSION_URL, state.getSessionName());
    }

    @Test
    public void closingTheDialogMarksItAsClosed() {
        OwnerCallDialogState state = new OwnerCallDialogState();
        state.displaySession(SESSION_URL);

        state.closeDialog();

        Assert.assertTrue(state.isDialogClosed());
    }

    @Test
    public void reopeningTheDialogMarksItAsOpen() {
        OwnerCallDialogState state = new OwnerCallDialogState();
        state.displaySession(SESSION_URL);
        state.closeDialog();

        state.reopenDialog();

        Assert.assertFalse(state.isDialogClosed());
    }

    @Test
    public void switchingToAnotherSessionResetsTheClosedState() {
        OwnerCallDialogState state = new OwnerCallDialogState();
        state.displaySession(SESSION_URL);
        state.closeDialog();

        state.displaySession(OTHER_SESSION_URL);

        Assert.assertFalse(state.isDialogClosed());
    }

    @Test
    public void allCallsRemainAfterDialogClose() {
        OwnerCallDialogState state = new OwnerCallDialogState();
        state.displaySession(SESSION_URL);

        state.closeDialog();

        Assert.assertEquals(2, state.indexOfDisplayedCall(THREE_CALLS));
    }
}
