package com.termux.app.terminal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SessionOpenRepaintDecisionTest {

    @Test
    public void runningSessionWithValidEmulatorSizeForcesRepaint() {
        assertTrue(SessionOpenRepaintDecision.shouldForceRemoteRepaint(true, true, 80, 24));
    }

    @Test
    public void notRunningSessionDoesNotForceRepaint() {
        assertFalse(SessionOpenRepaintDecision.shouldForceRemoteRepaint(false, true, 80, 24));
    }

    @Test
    public void missingEmulatorDoesNotForceRepaint() {
        assertFalse(SessionOpenRepaintDecision.shouldForceRemoteRepaint(true, false, 80, 24));
    }

    @Test
    public void zeroColumnsDoesNotForceRepaint() {
        assertFalse(SessionOpenRepaintDecision.shouldForceRemoteRepaint(true, true, 0, 24));
    }

    @Test
    public void zeroRowsDoesNotForceRepaint() {
        assertFalse(SessionOpenRepaintDecision.shouldForceRemoteRepaint(true, true, 80, 0));
    }

    @Test
    public void negativeDimensionsDoNotForceRepaint() {
        assertFalse(SessionOpenRepaintDecision.shouldForceRemoteRepaint(true, true, -1, -1));
    }
}
