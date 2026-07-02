package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

public class BrowserReconnectVisibilityRestorationScenarioTest {

    private static final String SESSION_NAME = "work-session";
    private static final String OLD_HANDLE = "handle-old";
    private static final String NEW_HANDLE = "handle-new";

    @Test
    public void reconnectKeepsTheBrowserOpenFlagSoTheReconnectedSessionRestoresItsBrowser() {
        BrowserSessionVisibilityState state = openBrowserForOldHandle();

        removeOldHandle(state, BrowserSessionRemovalReason.RECONNECT);

        Assert.assertTrue("reconnect must keep the session's browser-open flag so the restored tabs stay visible",
            state.wasBrowserOpenForSessionName(SESSION_NAME));
        Assert.assertFalse("the dead handle must no longer be remembered as browser-visible",
            state.wasBrowserVisible(OLD_HANDLE));
        Assert.assertFalse("the fresh handle starts without a handle-scoped visibility flag",
            state.wasBrowserVisible(NEW_HANDLE));
    }

    @Test
    public void genuineUserCloseDropsTheBrowserOpenFlagSoNothingIsRestored() {
        BrowserSessionVisibilityState state = openBrowserForOldHandle();

        removeOldHandle(state, BrowserSessionRemovalReason.USER_CLOSE);

        Assert.assertFalse("a genuine user close must forget the session's browser-open flag",
            state.wasBrowserOpenForSessionName(SESSION_NAME));
        Assert.assertFalse("a closed session with no browser-open flag must not restore its browser",
            state.shouldRestoreBrowserOnSessionChange(NEW_HANDLE, true));
    }

    private static BrowserSessionVisibilityState openBrowserForOldHandle() {
        BrowserSessionVisibilityState state = new BrowserSessionVisibilityState();
        state.setBrowserVisible(OLD_HANDLE, SESSION_NAME, true);
        return state;
    }

    private static void removeOldHandle(BrowserSessionVisibilityState state,
                                        BrowserSessionRemovalReason reason) {
        if (BrowserSessionRemovalVisibilityRetention.shouldClearBrowserOpenSessionName(reason)) {
            state.clearSession(OLD_HANDLE, SESSION_NAME);
        } else {
            state.clearSession(OLD_HANDLE);
        }
    }
}
