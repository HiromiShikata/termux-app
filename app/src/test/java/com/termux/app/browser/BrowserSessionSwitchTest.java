package com.termux.app.browser;

import org.junit.Assert;
import org.junit.Test;

public class BrowserSessionSwitchTest {

    @Test
    public void switchingToADifferentSessionRequiresTerminal() {
        Assert.assertTrue(
            BrowserSessionSwitch.requiresTerminalOnSessionChange("session-a", "session-b"));
    }

    @Test
    public void refreshingTheSameSessionDoesNotRequireTerminal() {
        Assert.assertFalse(
            BrowserSessionSwitch.requiresTerminalOnSessionChange("session-a", "session-a"));
    }

    @Test
    public void selectingTheFirstSessionRequiresTerminal() {
        Assert.assertTrue(
            BrowserSessionSwitch.requiresTerminalOnSessionChange(null, "session-a"));
    }

    @Test
    public void clearingTheActiveSessionRequiresTerminal() {
        Assert.assertTrue(
            BrowserSessionSwitch.requiresTerminalOnSessionChange("session-a", null));
    }

    @Test
    public void havingNoSessionBeforeOrAfterDoesNotRequireTerminal() {
        Assert.assertFalse(
            BrowserSessionSwitch.requiresTerminalOnSessionChange(null, null));
    }
}
