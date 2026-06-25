package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class TermuxTerminalSessionActivityClientTest {

    @Test
    public void copiesSessionNameWhenNameIsPresent() {
        Assert.assertEquals("workSession",
            TermuxTerminalSessionActivityClient.resolveSessionNameForCopy("workSession", "bash"));
    }

    @Test
    public void fallsBackToTitleWhenNameIsNull() {
        Assert.assertEquals("bash",
            TermuxTerminalSessionActivityClient.resolveSessionNameForCopy(null, "bash"));
    }

    @Test
    public void fallsBackToTitleWhenNameIsEmpty() {
        Assert.assertEquals("bash",
            TermuxTerminalSessionActivityClient.resolveSessionNameForCopy("", "bash"));
    }

    @Test
    public void returnsNullWhenBothNameAndTitleAreNull() {
        Assert.assertNull(
            TermuxTerminalSessionActivityClient.resolveSessionNameForCopy(null, null));
    }

    @Test
    public void returnsNullWhenBothNameAndTitleAreEmpty() {
        Assert.assertNull(
            TermuxTerminalSessionActivityClient.resolveSessionNameForCopy("", ""));
    }

    @Test
    public void wrapAroundSessionIndexAdvancesForwardWithinRange() {
        Assert.assertEquals(2,
            TermuxTerminalSessionActivityClient.wrapAroundSessionIndex(1, 3, true));
    }

    @Test
    public void wrapAroundSessionIndexWrapsToZeroPastTheEnd() {
        Assert.assertEquals(0,
            TermuxTerminalSessionActivityClient.wrapAroundSessionIndex(2, 3, true));
    }

    @Test
    public void wrapAroundSessionIndexStepsBackwardWithinRange() {
        Assert.assertEquals(0,
            TermuxTerminalSessionActivityClient.wrapAroundSessionIndex(1, 3, false));
    }

    @Test
    public void wrapAroundSessionIndexWrapsToLastBeforeTheStart() {
        Assert.assertEquals(2,
            TermuxTerminalSessionActivityClient.wrapAroundSessionIndex(0, 3, false));
    }

    @Test
    public void closingNonCurrentSessionPreservesCurrentSession() {
        boolean finishedSessionWasCurrent = false;
        boolean currentSessionStillPresent = true;
        Assert.assertFalse(
            TermuxTerminalSessionActivityClient.shouldReselectCurrentSessionAfterRemoval(
                finishedSessionWasCurrent, currentSessionStillPresent));
    }

    @Test
    public void closingCurrentSessionSelectsNextVisibleSession() {
        boolean finishedSessionWasCurrent = true;
        boolean currentSessionStillPresent = false;
        Assert.assertTrue(
            TermuxTerminalSessionActivityClient.shouldReselectCurrentSessionAfterRemoval(
                finishedSessionWasCurrent, currentSessionStillPresent));
    }

    @Test
    public void reselectsWhenCurrentSessionNoLongerPresentEvenIfNotTheFinishedSession() {
        boolean finishedSessionWasCurrent = false;
        boolean currentSessionStillPresent = false;
        Assert.assertTrue(
            TermuxTerminalSessionActivityClient.shouldReselectCurrentSessionAfterRemoval(
                finishedSessionWasCurrent, currentSessionStillPresent));
    }

    @Test
    public void removesFinishedSessionOnNormalDeviceRegardlessOfExitStatus() {
        boolean isLeanbackDevice = false;
        Assert.assertTrue(
            TermuxTerminalSessionActivityClient.shouldRemoveFinishedSession(isLeanbackDevice, 1, false));
    }

    @Test
    public void keepsLastFinishedSessionOnLeanbackDeviceWithoutPendingPluginResult() {
        boolean isLeanbackDevice = true;
        Assert.assertFalse(
            TermuxTerminalSessionActivityClient.shouldRemoveFinishedSession(isLeanbackDevice, 1, false));
    }

    @Test
    public void removesFinishedSessionOnLeanbackDeviceWhenMoreSessionsRemain() {
        boolean isLeanbackDevice = true;
        Assert.assertTrue(
            TermuxTerminalSessionActivityClient.shouldRemoveFinishedSession(isLeanbackDevice, 2, false));
    }

    @Test
    public void removesFinishedSessionOnLeanbackDeviceForPluginCommandWithPendingResult() {
        boolean isLeanbackDevice = true;
        Assert.assertTrue(
            TermuxTerminalSessionActivityClient.shouldRemoveFinishedSession(isLeanbackDevice, 1, true));
    }
}
