package com.termux.view.scroll;

import org.junit.Assert;
import org.junit.Test;

public class TerminalScrollStepCounterTest {

    @Test
    public void aStepIsCountedAgainstTheDestinationItWentTo() {
        TerminalScrollStepCounter counter = new TerminalScrollStepCounter();

        counter.record(TerminalScrollEvent.MOUSE_WHEEL, 1000L);
        counter.record(TerminalScrollEvent.MOUSE_WHEEL, 1010L);
        counter.record(TerminalScrollEvent.LOCAL_SCROLLBACK, 1020L);

        Assert.assertEquals("a gesture the shell swallowed and a gesture that moved the view are the two"
                + " outcomes a reader has to tell apart, so they cannot share one count",
            2, counter.getStepCount(TerminalScrollEvent.MOUSE_WHEEL));
        Assert.assertEquals("a gesture the shell swallowed and a gesture that moved the view are the two"
                + " outcomes a reader has to tell apart, so they cannot share one count",
            1, counter.getStepCount(TerminalScrollEvent.LOCAL_SCROLLBACK));
    }

    @Test
    public void aDestinationNoStepWentToCountsZeroRatherThanBeingUnknown() {
        TerminalScrollStepCounter counter = new TerminalScrollStepCounter();

        counter.record(TerminalScrollEvent.MOUSE_WHEEL, 1000L);

        Assert.assertEquals("zero steps to a destination is the finding that the gesture never took that"
                + " route, which is different from the counter not knowing",
            0, counter.getStepCount(TerminalScrollEvent.ARROW_KEY));
    }

    @Test
    public void theMostRecentStepTimeIsKeptForEachDestinationSeparately() {
        TerminalScrollStepCounter counter = new TerminalScrollStepCounter();

        counter.record(TerminalScrollEvent.MOUSE_WHEEL, 1000L);
        counter.record(TerminalScrollEvent.LOCAL_SCROLLBACK, 2000L);
        counter.record(TerminalScrollEvent.MOUSE_WHEEL, 3000L);

        Assert.assertEquals("a reading is taken minutes after the owner reports scrolling dead, so the"
                + " time of the last step is what says whether the gesture happened in that window",
            3000L, counter.getLastStepAtMillis(TerminalScrollEvent.MOUSE_WHEEL));
        Assert.assertEquals("one destination going quiet while another keeps moving is the signal, so the"
                + " times cannot be shared",
            2000L, counter.getLastStepAtMillis(TerminalScrollEvent.LOCAL_SCROLLBACK));
    }

    @Test
    public void aDestinationNoStepWentToHasNoTimeToReport() {
        TerminalScrollStepCounter counter = new TerminalScrollStepCounter();

        counter.record(TerminalScrollEvent.MOUSE_WHEEL, 1000L);

        try {
            counter.getLastStepAtMillis(TerminalScrollEvent.LOCAL_SCROLLBACK);
            Assert.fail("a made-up time for a step that never happened would be read as a scroll the"
                + " owner never made, which is the opposite of what this counter exists to establish");
        } catch (IllegalStateException expected) {
            Assert.assertTrue("the failure has to name the destination so the reader can act on it,"
                    + " actual message: " + expected.getMessage(),
                expected.getMessage().contains(TerminalScrollEvent.LOCAL_SCROLLBACK.name()));
        }
    }

    @Test
    public void theTotalCountsEveryStepWhateverItsDestination() {
        TerminalScrollStepCounter counter = new TerminalScrollStepCounter();

        counter.record(TerminalScrollEvent.MOUSE_WHEEL, 1000L);
        counter.record(TerminalScrollEvent.ARROW_KEY, 1010L);
        counter.record(TerminalScrollEvent.LOCAL_SCROLLBACK, 1020L);
        counter.record(TerminalScrollEvent.MOUSE_WHEEL, 1030L);

        Assert.assertEquals("a total of zero is the one reading that proves no gesture reached the"
                + " scrolling code at all, so it has to cover every destination",
            4, counter.getTotalStepCount());
    }
}
