package com.termux.view.scroll;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

public class LatestTerminalScrollStepTest {

    @Test
    public void theMostRecentStepAcrossEveryDestinationIsTheOneReported() {
        TerminalScrollStepCounter counter = new TerminalScrollStepCounter();
        counter.record(TerminalScrollEvent.MOUSE_WHEEL, 50_000L);
        counter.record(TerminalScrollEvent.LOCAL_SCROLLBACK, 20_000L);

        LatestTerminalScrollStep latestStep = LatestTerminalScrollStep.of(counter);

        assertTrue(latestStep.hasStepped());
        assertEquals("reading a destination that was stepped earlier as the latest scroll would"
                + " make a terminal that is being scrolled right now look like one that was last"
                + " scrolled long ago",
            50_000L, latestStep.getSteppedAtMillis());
    }

    @Test
    public void aDestinationThatWasNeverSteppedIsNotAskedForItsTime() {
        TerminalScrollStepCounter counter = new TerminalScrollStepCounter();
        counter.record(TerminalScrollEvent.ARROW_KEY, 30_000L);

        LatestTerminalScrollStep latestStep = LatestTerminalScrollStep.of(counter);

        assertEquals("the counter throws when a destination with no steps is asked for its last"
                + " step, and this runs on the background cycle of the running application, so"
                + " asking unconditionally would break that cycle for a diagnostics reading",
            30_000L, latestStep.getSteppedAtMillis());
    }

    @Test
    public void nothingIsReportedWhenNoDestinationHasBeenSteppedYet() {
        TerminalScrollStepCounter counter = new TerminalScrollStepCounter();

        LatestTerminalScrollStep latestStep = LatestTerminalScrollStep.of(counter);

        assertFalse("a terminal that was never scrolled has no scroll to compare a draw against,"
                + " and reporting one would put an episode in the record that never happened",
            latestStep.hasStepped());
    }

    @Test
    public void askingForTheTimeOfAStepThatNeverHappenedFailsRatherThanReturningAStandInValue() {
        TerminalScrollStepCounter counter = new TerminalScrollStepCounter();

        LatestTerminalScrollStep latestStep = LatestTerminalScrollStep.of(counter);

        try {
            latestStep.getSteppedAtMillis();
            fail("a stand-in time for a scroll that never happened would be subtracted from a draw"
                + " time and reported as an episode lasting decades");
        } catch (IllegalStateException expected) {
        }
    }
}
