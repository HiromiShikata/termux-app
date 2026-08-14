package com.termux.app.ownercall;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

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
    public void startsAtTheOldestCallOfANewlyDisplayedSession() {
        OwnerCallDialogState state = new OwnerCallDialogState();
        state.displaySession(SESSION_URL);
        state.displayCallAt(THREE_CALLS, 2);

        state.displaySession(OTHER_SESSION_URL);

        Assert.assertEquals(0, state.indexOfDisplayedCall(THREE_CALLS));
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

        Assert.assertEquals(0, state.indexOfDisplayedCall(THREE_CALLS));
    }
}
