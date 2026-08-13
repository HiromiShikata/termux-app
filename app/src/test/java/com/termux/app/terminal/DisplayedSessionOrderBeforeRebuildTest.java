package com.termux.app.terminal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DisplayedSessionOrderBeforeRebuildTest {

    @Test
    public void thePositionOfTheDisplayedSessionIsWhereItSatInTheOrderTheOwnerSaw() {
        DisplayedSessionOrderBeforeRebuild order = DisplayedSessionOrderBeforeRebuild.of(
            Arrays.asList("first", "second", "third"), "second");

        assertEquals("the anchor must be the row the owner was looking at",
            1, order.getPositionOfDisplayedSession());
    }

    @Test
    public void aDisplayedSessionThatWasNotInTheListHasNoPosition() {
        DisplayedSessionOrderBeforeRebuild order = DisplayedSessionOrderBeforeRebuild.of(
            Arrays.asList("first", "second"), "never-listed");

        assertEquals("a session absent from the order must not be given an invented position",
            NearestNeighborSessionOrder.NO_POSITION, order.getPositionOfDisplayedSession());
    }

    @Test
    public void anOrderThatWasNeverCapturedHasNoNamesAndNoPosition() {
        DisplayedSessionOrderBeforeRebuild order = DisplayedSessionOrderBeforeRebuild.NOT_CAPTURED;

        assertTrue("an uncaptured order must carry no names", order.getDisplayedSessionNames().isEmpty());
        assertEquals("an uncaptured order must carry no position",
            NearestNeighborSessionOrder.NO_POSITION, order.getPositionOfDisplayedSession());
    }

    @Test
    public void theCapturedOrderIsNotChangedByLaterEditsToTheListItWasBuiltFrom() {
        List<String> displayedSessionNames = new ArrayList<>(Arrays.asList("first", "second"));
        DisplayedSessionOrderBeforeRebuild order =
            DisplayedSessionOrderBeforeRebuild.of(displayedSessionNames, "second");

        displayedSessionNames.clear();

        assertEquals("the order captured before the rebuild must survive the rebuild that follows it",
            Arrays.asList("first", "second"), order.getDisplayedSessionNames());
        assertEquals("the anchor captured before the rebuild must survive the rebuild that follows it",
            1, order.getPositionOfDisplayedSession());
    }
}
