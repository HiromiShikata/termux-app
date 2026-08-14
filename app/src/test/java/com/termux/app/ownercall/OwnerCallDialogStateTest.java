package com.termux.app.ownercall;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class OwnerCallDialogStateTest {

    private static final String SESSION_URL =
        "https://github.com/HiromiShikata/termux-app/issues/1884";
    private static final String OTHER_SESSION_URL =
        "https://github.com/HiromiShikata/termux-app/issues/1885";
    private static final String REPEATED_BODY =
        "Decide whether the invoice recipient may be changed.";

    private static OwnerCall call(String sessionName, String calledAt, String body) {
        return new OwnerCall(sessionName, calledAt, body);
    }

    private static final OwnerCall OLDEST_CALL =
        call(SESSION_URL, "2026-08-14T04:00:00Z", "Decide whether the addresses may be deleted.");
    private static final OwnerCall MIDDLE_CALL =
        call(SESSION_URL, "2026-08-14T04:03:00Z", REPEATED_BODY);
    private static final OwnerCall NEWEST_CALL =
        call(SESSION_URL, "2026-08-14T04:05:00Z", REPEATED_BODY);
    private static final List<OwnerCall> THREE_CALLS =
        Arrays.asList(OLDEST_CALL, MIDDLE_CALL, NEWEST_CALL);

    @Test
    public void tellsTwoCallsOfTheSameTextApartByTheMomentTheOwnerWasCalled() {
        OwnerCallDialogState state = new OwnerCallDialogState();
        state.displaySession(SESSION_URL);

        state.dismiss(NEWEST_CALL);

        Assert.assertEquals(Arrays.asList(OLDEST_CALL, MIDDLE_CALL),
            state.visibleCalls(THREE_CALLS));
    }

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
    public void closingACallOfOneSessionLeavesTheSameCallTimeOfAnotherSessionVisible() {
        OwnerCall otherSessionCall =
            call(OTHER_SESSION_URL, NEWEST_CALL.getCalledAt(), REPEATED_BODY);
        OwnerCallDialogState state = new OwnerCallDialogState();
        state.displaySession(SESSION_URL);
        state.dismiss(NEWEST_CALL);

        state.displaySession(OTHER_SESSION_URL);

        Assert.assertEquals(Collections.singletonList(otherSessionCall),
            state.visibleCalls(Collections.singletonList(otherSessionCall)));
    }

    @Test
    public void showsTheOldestRemainingCallAfterTheDisplayedOneIsClosed() {
        OwnerCallDialogState state = new OwnerCallDialogState();
        state.displaySession(SESSION_URL);
        state.displayCallAt(THREE_CALLS, 2);

        state.dismiss(NEWEST_CALL);

        Assert.assertEquals(0, state.indexOfDisplayedCall(state.visibleCalls(THREE_CALLS)));
    }

    @Test
    public void remembersWhichSessionIsDisplayed() {
        OwnerCallDialogState state = new OwnerCallDialogState();

        state.displaySession(SESSION_URL);

        Assert.assertEquals(SESSION_URL, state.getSessionName());
    }
}
