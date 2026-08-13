package com.termux.view.touch;

import org.junit.Assert;
import org.junit.Test;

public class TerminalTouchCounterTest {

    @Test
    public void aTouchIsCountedAgainstTheKindItWas() {
        TerminalTouchCounter counter = new TerminalTouchCounter();

        counter.record(TerminalTouchKind.GESTURE_START, 1000L);
        counter.record(TerminalTouchKind.GESTURE_MOVEMENT, 1100L);
        counter.record(TerminalTouchKind.GESTURE_MOVEMENT, 1200L);

        Assert.assertEquals("a gesture that started once must not read as having started twice",
            1, counter.getTouchCount(TerminalTouchKind.GESTURE_START));
        Assert.assertEquals("movement is what tells a scroll attempt from a tap, so it is counted"
            + " separately", 2, counter.getTouchCount(TerminalTouchKind.GESTURE_MOVEMENT));
    }

    @Test
    public void aKindNoTouchArrivedAsCountsZeroRatherThanBeingUnknown() {
        TerminalTouchCounter counter = new TerminalTouchCounter();

        counter.record(TerminalTouchKind.GESTURE_START, 1000L);

        Assert.assertEquals("a kind nothing arrived as is a measured zero, which is the reading that"
                + " says the terminal view was never touched that way",
            0, counter.getTouchCount(TerminalTouchKind.GESTURE_MOVEMENT));
    }

    @Test
    public void theMostRecentTouchTimeIsKeptForEachKindSeparately() {
        TerminalTouchCounter counter = new TerminalTouchCounter();

        counter.record(TerminalTouchKind.GESTURE_START, 1000L);
        counter.record(TerminalTouchKind.GESTURE_MOVEMENT, 1500L);
        counter.record(TerminalTouchKind.GESTURE_START, 2000L);

        Assert.assertEquals("the latest start is what places the last attempt in time",
            2000L, counter.getLastTouchAtMillis(TerminalTouchKind.GESTURE_START));
        Assert.assertEquals("a later touch of another kind must not overwrite this one's time",
            1500L, counter.getLastTouchAtMillis(TerminalTouchKind.GESTURE_MOVEMENT));
    }

    @Test
    public void aKindNoTouchArrivedAsHasNoTimeToReport() {
        TerminalTouchCounter counter = new TerminalTouchCounter();

        try {
            counter.getLastTouchAtMillis(TerminalTouchKind.GESTURE_END);
            Assert.fail("returning a stand-in time would read as a touch the owner never made");
        } catch (IllegalStateException expected) {
            Assert.assertTrue("the message has to name the kind so the reading can be acted on,"
                    + " but it was: " + expected.getMessage(),
                expected.getMessage().contains("GESTURE_END"));
        }
    }

    @Test
    public void theTotalCountsEveryTouchWhateverItsKind() {
        TerminalTouchCounter counter = new TerminalTouchCounter();

        counter.record(TerminalTouchKind.GESTURE_START, 1000L);
        counter.record(TerminalTouchKind.GESTURE_MOVEMENT, 1100L);
        counter.record(TerminalTouchKind.GESTURE_END, 1200L);
        counter.record(TerminalTouchKind.ANOTHER_KIND, 1300L);

        Assert.assertEquals("the total is what rules out the terminal view having been touched at all",
            4, counter.getTotalTouchCount());
    }
}
