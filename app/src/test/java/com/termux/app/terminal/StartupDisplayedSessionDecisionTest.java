package com.termux.app.terminal;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class StartupDisplayedSessionDecisionTest {

    @Test
    public void topmostNonHiddenSessionWinsOverTheCallersOwnChoice() {
        assertEquals(2, StartupDisplayedSessionDecision.sessionIndexToDisplay(2, 0));
    }

    @Test
    public void topmostNonHiddenSessionAtTheVeryTopIsStillHonoured() {
        assertEquals(0, StartupDisplayedSessionDecision.sessionIndexToDisplay(0, 3));
    }

    @Test
    public void callersOwnChoiceIsKeptWhenNoNonHiddenSessionExists() {
        assertEquals(4, StartupDisplayedSessionDecision.sessionIndexToDisplay(-1, 4));
    }

    @Test
    public void nothingIsSelectedWhenNeitherANonHiddenSessionNorAFallbackExists() {
        assertEquals(-1, StartupDisplayedSessionDecision.sessionIndexToDisplay(-1, -1));
    }
}
