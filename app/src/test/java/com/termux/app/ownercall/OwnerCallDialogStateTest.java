package com.termux.app.ownercall;

import static org.junit.Assert.assertEquals;

import com.termux.app.sessiondefinition.UnansweredOwnerCall;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class OwnerCallDialogStateTest {

    private static final String SESSION =
        "https://github.com/HiromiShikata/termux-app/issues/1884";
    private static final String OTHER_SESSION =
        "https://github.com/HiromiShikata/termux-app/issues/1885";
    private static final UnansweredOwnerCall FIRST_CALL =
        new UnansweredOwnerCall("2027-01-15T08:00:00.000Z", "approve the address deletion");
    private static final UnansweredOwnerCall SECOND_CALL =
        new UnansweredOwnerCall("2027-01-15T08:04:00.000Z", "approve the address deletion");
    private static final UnansweredOwnerCall THIRD_CALL =
        new UnansweredOwnerCall("2027-01-15T08:09:00.000Z", "approve the invoice address change");
    private static final List<UnansweredOwnerCall> BOTH_CALLS =
        Arrays.asList(FIRST_CALL, SECOND_CALL);
    private static final List<UnansweredOwnerCall> THREE_CALLS =
        Arrays.asList(FIRST_CALL, SECOND_CALL, THIRD_CALL);

    private static OwnerCallDialogState stateShowing(String sessionName) {
        OwnerCallDialogState state = new OwnerCallDialogState();
        state.displaySession(sessionName);
        return state;
    }

    @Test
    public void showsEveryCallBeforeAnyIsClosed() {
        OwnerCallDialogState state = stateShowing(SESSION);

        assertEquals(BOTH_CALLS, state.visibleCalls(BOTH_CALLS));
        assertEquals(0, state.indexOfDisplayedCall(BOTH_CALLS));
    }

    @Test
    public void keepsTheOtherCallVisibleAfterOneIsClosedEvenWhenTheBodiesAreIdentical() {
        OwnerCallDialogState state = stateShowing(SESSION);

        state.dismiss(FIRST_CALL);

        assertEquals(Collections.singletonList(SECOND_CALL), state.visibleCalls(BOTH_CALLS));
    }

    @Test
    public void keepsAClosedCallClosedOnEveryLaterRefresh() {
        OwnerCallDialogState state = stateShowing(SESSION);

        state.dismiss(SECOND_CALL);

        assertEquals(Collections.singletonList(FIRST_CALL), state.visibleCalls(BOTH_CALLS));
        assertEquals(Collections.singletonList(FIRST_CALL), state.visibleCalls(BOTH_CALLS));
    }

    @Test
    public void closingACallOfOneSessionLeavesTheSameCallTimeOfAnotherSessionVisible() {
        OwnerCallDialogState state = stateShowing(SESSION);

        state.dismiss(FIRST_CALL);
        state.displaySession(OTHER_SESSION);

        assertEquals(BOTH_CALLS, state.visibleCalls(BOTH_CALLS));
    }

    @Test
    public void returnsToTheFirstCallAfterOneIsClosed() {
        OwnerCallDialogState state = stateShowing(SESSION);

        state.displayCallAt(BOTH_CALLS, 1);
        state.dismiss(SECOND_CALL);

        assertEquals(0, state.indexOfDisplayedCall(Collections.singletonList(FIRST_CALL)));
    }

    @Test
    public void keepsShowingTheSameCallWhenAnEarlierCallIsAnsweredAndDropsOutOfTheDocument() {
        OwnerCallDialogState state = stateShowing(SESSION);

        state.displayCallAt(THREE_CALLS, 2);

        assertEquals(1, state.indexOfDisplayedCall(Arrays.asList(SECOND_CALL, THIRD_CALL)));
    }

    @Test
    public void startsAtTheOldestCallOfANewlyDisplayedSession() {
        OwnerCallDialogState state = stateShowing(SESSION);

        state.displayCallAt(THREE_CALLS, 2);
        state.displaySession(OTHER_SESSION);

        assertEquals(0, state.indexOfDisplayedCall(THREE_CALLS));
    }

    @Test
    public void keepsTheDisplayedCallWhenTheSameSessionIsRenderedAgain() {
        OwnerCallDialogState state = stateShowing(SESSION);

        state.displayCallAt(THREE_CALLS, 2);
        state.displaySession(SESSION);

        assertEquals(2, state.indexOfDisplayedCall(THREE_CALLS));
    }

    @Test
    public void fallsBackToTheOldestCallWhenTheDisplayedCallIsNoLongerWaiting() {
        OwnerCallDialogState state = stateShowing(SESSION);

        state.displayCallAt(THREE_CALLS, 2);

        assertEquals(0, state.indexOfDisplayedCall(BOTH_CALLS));
    }
}
