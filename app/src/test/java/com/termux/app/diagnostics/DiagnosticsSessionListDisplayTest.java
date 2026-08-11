package com.termux.app.diagnostics;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

public class DiagnosticsSessionListDisplayTest {

    @Test
    public void aSessionTheListRendersIsDisplayed() {
        Assert.assertEquals(DiagnosticsSessionListDisplay.DISPLAYED,
            DiagnosticsSessionListDisplay.ofSessionIndex(3, Arrays.asList(1, 3, 5)));
    }

    @Test
    public void aSessionTheListLeavesOutIsNotDisplayed() {
        Assert.assertEquals("a session the list does not render is the case a reader needs to separate a"
                + " session deliberately left without a screen from one that is on screen and dead",
            DiagnosticsSessionListDisplay.NOT_DISPLAYED,
            DiagnosticsSessionListDisplay.ofSessionIndex(4, Arrays.asList(1, 3, 5)));
    }

    @Test
    public void aReadingTakenWhileNoSessionListExistsSaysSoRatherThanClaimingTheSessionIsNotDisplayed() {
        Assert.assertEquals("reporting an unbuilt list as not displayed would state an observation the"
                + " application never made, and every session would read as hidden",
            DiagnosticsSessionListDisplay.NOT_KNOWN,
            DiagnosticsSessionListDisplay.ofSessionIndex(0, null));
    }
}
