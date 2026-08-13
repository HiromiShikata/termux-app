package com.termux.app.ownercall;

import static org.junit.Assert.assertEquals;

import com.termux.app.sessiondefinition.UnansweredOwnerCall;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class OwnerCallDialogStateTest {

    private static final UnansweredOwnerCall FIRST_CALL =
        new UnansweredOwnerCall("2027-01-15T08:00:00.000Z", "approve the address deletion");
    private static final UnansweredOwnerCall SECOND_CALL =
        new UnansweredOwnerCall("2027-01-15T08:04:00.000Z", "approve the address deletion");
    private static final List<UnansweredOwnerCall> BOTH_CALLS =
        Arrays.asList(FIRST_CALL, SECOND_CALL);

    @Test
    public void showsEveryCallBeforeAnyIsClosed() {
        OwnerCallDialogState state = new OwnerCallDialogState();

        assertEquals(BOTH_CALLS, state.visibleCalls(BOTH_CALLS));
        assertEquals(0, state.getIndex());
    }

    @Test
    public void keepsTheOtherCallVisibleAfterOneIsClosedEvenWhenTheBodiesAreIdentical() {
        OwnerCallDialogState state = new OwnerCallDialogState();

        state.dismiss(FIRST_CALL);

        assertEquals(Collections.singletonList(SECOND_CALL), state.visibleCalls(BOTH_CALLS));
    }

    @Test
    public void keepsAClosedCallClosedOnEveryLaterRefresh() {
        OwnerCallDialogState state = new OwnerCallDialogState();

        state.dismiss(SECOND_CALL);

        assertEquals(Collections.singletonList(FIRST_CALL), state.visibleCalls(BOTH_CALLS));
        assertEquals(Collections.singletonList(FIRST_CALL), state.visibleCalls(BOTH_CALLS));
    }

    @Test
    public void returnsToTheFirstCallAfterOneIsClosed() {
        OwnerCallDialogState state = new OwnerCallDialogState();

        state.showNextCall();
        state.dismiss(SECOND_CALL);

        assertEquals(0, state.getIndex());
    }

    @Test
    public void walksForwardAndBackThroughTheWaitingCalls() {
        OwnerCallDialogState state = new OwnerCallDialogState();

        state.showNextCall();
        assertEquals(1, state.getIndex());

        state.showPreviousCall();
        assertEquals(0, state.getIndex());
    }

    @Test
    public void adoptsTheIndexTheDialogActuallySettledOn() {
        OwnerCallDialogState state = new OwnerCallDialogState();

        state.showNextCall();
        state.showNextCall();
        state.applyResolvedIndex(OwnerCallDialogPaging.resolve(state.getIndex(), 2).getIndex());

        assertEquals(1, state.getIndex());
    }
}
