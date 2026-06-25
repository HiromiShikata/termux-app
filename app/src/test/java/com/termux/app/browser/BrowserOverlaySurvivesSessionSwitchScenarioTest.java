package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

public class BrowserOverlaySurvivesSessionSwitchScenarioTest {

    private static final String SESSION_A = "session-a";
    private static final String SESSION_B = "session-b";
    private static final String NAME_A = "name-a";
    private static final String NAME_B = "name-b";

    private void openBrowser(BrowserSessionVisibilityState state, String handle, String name) {
        state.setBrowserVisible(handle, name, true);
    }

    private void closeBrowserToTerminal(BrowserSessionVisibilityState state, String handle, String name) {
        state.setBrowserVisible(handle, name, false);
    }

    private boolean restoreBrowserOnReturn(
        BrowserSessionVisibilityState state, String handle, String name, boolean hasActiveTab) {
        return state.shouldRestoreBrowserOnSessionChange(handle, hasActiveTab)
            || (hasActiveTab && state.wasBrowserOpenForSessionName(name));
    }

    @Test
    public void browserOpenInSessionAStaysOpenAfterSwitchingToBAndBackViaSessionOverlay() {
        BrowserSessionVisibilityState state = new BrowserSessionVisibilityState();

        openBrowser(state, SESSION_A, NAME_A);

        Assert.assertTrue(restoreBrowserOnReturn(state, SESSION_A, NAME_A, true));

        switchToSessionTerminal(state, SESSION_B, NAME_B);

        Assert.assertTrue(restoreBrowserOnReturn(state, SESSION_A, NAME_A, true));
    }

    @Test
    public void switchingToASessionThatNeverOpenedTheBrowserShowsTerminal() {
        BrowserSessionVisibilityState state = new BrowserSessionVisibilityState();

        openBrowser(state, SESSION_A, NAME_A);
        switchToSessionTerminal(state, SESSION_B, NAME_B);

        Assert.assertFalse(restoreBrowserOnReturn(state, SESSION_B, NAME_B, true));
    }

    @Test
    public void deliberatelyClosingTheBrowserInSessionAKeepsItClosedAfterReturning() {
        BrowserSessionVisibilityState state = new BrowserSessionVisibilityState();

        openBrowser(state, SESSION_A, NAME_A);
        closeBrowserToTerminal(state, SESSION_A, NAME_A);

        switchToSessionTerminal(state, SESSION_B, NAME_B);

        Assert.assertFalse(restoreBrowserOnReturn(state, SESSION_A, NAME_A, true));
    }

    private void switchToSessionTerminal(
        BrowserSessionVisibilityState state, String targetHandle, String targetName) {
        if (!restoreBrowserOnReturn(state, targetHandle, targetName, false)) {
            state.setBrowserVisible(targetHandle, targetName, false);
        }
    }
}
