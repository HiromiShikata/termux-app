package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

public class BrowserSessionVisibilityStateTest {

    private static final String SESSION_A = "session-a";
    private static final String SESSION_B = "session-b";

    @Test
    public void aNewlySeenSessionDefaultsToTerminal() {
        BrowserSessionVisibilityState state = new BrowserSessionVisibilityState();
        Assert.assertFalse(state.wasBrowserVisible(SESSION_A));
    }

    @Test
    public void rememberingBrowserVisibleIsScopedPerSession() {
        BrowserSessionVisibilityState state = new BrowserSessionVisibilityState();
        state.setBrowserVisible(SESSION_A, true);
        Assert.assertTrue(state.wasBrowserVisible(SESSION_A));
        Assert.assertFalse(state.wasBrowserVisible(SESSION_B));
    }

    @Test
    public void closingTheBrowserClearsTheRememberedFlagForThatSession() {
        BrowserSessionVisibilityState state = new BrowserSessionVisibilityState();
        state.setBrowserVisible(SESSION_A, true);
        state.setBrowserVisible(SESSION_A, false);
        Assert.assertFalse(state.wasBrowserVisible(SESSION_A));
    }

    @Test
    public void restoresBrowserWhenTargetHadBrowserVisibleAndHasActiveTab() {
        BrowserSessionVisibilityState state = new BrowserSessionVisibilityState();
        state.setBrowserVisible(SESSION_A, true);
        Assert.assertTrue(state.shouldRestoreBrowserOnSessionChange(SESSION_A, true));
    }

    @Test
    public void showsTerminalWhenTargetHadBrowserVisibleButHasNoActiveTab() {
        BrowserSessionVisibilityState state = new BrowserSessionVisibilityState();
        state.setBrowserVisible(SESSION_A, true);
        Assert.assertFalse(state.shouldRestoreBrowserOnSessionChange(SESSION_A, false));
    }

    @Test
    public void showsTerminalWhenTargetWasShowingTerminalEvenWithAnActiveTab() {
        BrowserSessionVisibilityState state = new BrowserSessionVisibilityState();
        Assert.assertFalse(state.shouldRestoreBrowserOnSessionChange(SESSION_A, true));
    }

    @Test
    public void clearingASessionForgetsItsRememberedBrowserState() {
        BrowserSessionVisibilityState state = new BrowserSessionVisibilityState();
        state.setBrowserVisible(SESSION_A, true);
        state.clearSession(SESSION_A);
        Assert.assertFalse(state.wasBrowserVisible(SESSION_A));
        Assert.assertFalse(state.shouldRestoreBrowserOnSessionChange(SESSION_A, true));
    }

    @Test
    public void aNullSessionHandleIsNeverBrowserVisible() {
        BrowserSessionVisibilityState state = new BrowserSessionVisibilityState();
        state.setBrowserVisible(null, true);
        Assert.assertFalse(state.wasBrowserVisible(null));
        Assert.assertFalse(state.shouldRestoreBrowserOnSessionChange(null, true));
    }

    @Test
    public void rememberedStatesOfTwoSessionsAreIndependent() {
        BrowserSessionVisibilityState state = new BrowserSessionVisibilityState();
        state.setBrowserVisible(SESSION_A, true);
        state.setBrowserVisible(SESSION_B, false);
        Assert.assertTrue(state.shouldRestoreBrowserOnSessionChange(SESSION_A, true));
        Assert.assertFalse(state.shouldRestoreBrowserOnSessionChange(SESSION_B, true));
    }
}
