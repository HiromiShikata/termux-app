package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

public class ActiveSessionViewBindingResolutionTest {

    private static final String SESSION_A = "session-a";
    private static final String SESSION_B = "session-b";

    @Test
    public void terminalAndBrowserBothMatchingTheActiveSessionIsConsistent() {
        ActiveSessionViewBindingResolution resolution = ActiveSessionViewBindingResolution.resolve(
            SESSION_A, SESSION_A, true, SESSION_A, true);

        Assert.assertTrue(resolution.isConsistent());
        Assert.assertFalse(resolution.requiresTerminalRebind());
        Assert.assertFalse(resolution.requiresBrowserRebind());
    }

    @Test
    public void aStaleTerminalShowingAPreviousSessionRequiresTerminalRebind() {
        ActiveSessionViewBindingResolution resolution = ActiveSessionViewBindingResolution.resolve(
            SESSION_A, SESSION_B, false, null, false);

        Assert.assertTrue(resolution.requiresTerminalRebind());
        Assert.assertFalse(resolution.isConsistent());
    }

    @Test
    public void aTerminalThatHasNotYetAttachedTheActiveSessionRequiresTerminalRebind() {
        ActiveSessionViewBindingResolution resolution = ActiveSessionViewBindingResolution.resolve(
            SESSION_A, null, false, null, false);

        Assert.assertTrue(resolution.requiresTerminalRebind());
    }

    @Test
    public void aVisibleBrowserShowingAnotherSessionsPageRequiresBrowserRebind() {
        ActiveSessionViewBindingResolution resolution = ActiveSessionViewBindingResolution.resolve(
            SESSION_A, SESSION_A, true, SESSION_B, true);

        Assert.assertFalse(resolution.requiresTerminalRebind());
        Assert.assertTrue(resolution.requiresBrowserRebind());
    }

    @Test
    public void aVisibleBrowserWhoseActiveSessionTabIsNotYetShownRequiresBrowserRebind() {
        ActiveSessionViewBindingResolution resolution = ActiveSessionViewBindingResolution.resolve(
            SESSION_A, SESSION_A, true, null, true);

        Assert.assertTrue(resolution.requiresBrowserRebind());
    }

    @Test
    public void aHiddenBrowserStillOwningAPreviousSessionsPageDoesNotRequireRebind() {
        ActiveSessionViewBindingResolution resolution = ActiveSessionViewBindingResolution.resolve(
            SESSION_A, SESSION_A, false, SESSION_B, false);

        Assert.assertTrue(resolution.isConsistent());
        Assert.assertFalse(resolution.requiresBrowserRebind());
    }

    @Test
    public void aVisibleBrowserWithoutATabForTheActiveSessionAndNoForeignPageIsConsistent() {
        ActiveSessionViewBindingResolution resolution = ActiveSessionViewBindingResolution.resolve(
            SESSION_A, SESSION_A, true, null, false);

        Assert.assertTrue(resolution.isConsistent());
    }

    @Test
    public void bothSurfacesStaleRequiresBothRebinds() {
        ActiveSessionViewBindingResolution resolution = ActiveSessionViewBindingResolution.resolve(
            SESSION_A, SESSION_B, true, SESSION_B, true);

        Assert.assertTrue(resolution.requiresTerminalRebind());
        Assert.assertTrue(resolution.requiresBrowserRebind());
    }

    @Test
    public void aNullActiveSessionNeverRequiresATerminalRebind() {
        ActiveSessionViewBindingResolution resolution = ActiveSessionViewBindingResolution.resolve(
            null, null, false, null, false);

        Assert.assertFalse(resolution.requiresTerminalRebind());
        Assert.assertTrue(resolution.isConsistent());
    }

    @Test
    public void theDiagnosticLineReportsTheActiveTerminalAndBrowserHandles() {
        ActiveSessionViewBindingResolution resolution = ActiveSessionViewBindingResolution.resolve(
            SESSION_A, SESSION_B, true, SESSION_B, true);

        String diagnosticLine = resolution.diagnosticLine();
        Assert.assertTrue(diagnosticLine.contains("active=" + SESSION_A));
        Assert.assertTrue(diagnosticLine.contains("terminal=" + SESSION_B));
        Assert.assertTrue(diagnosticLine.contains("browser=" + SESSION_B));
        Assert.assertTrue(diagnosticLine.contains("terminalRebind=true"));
        Assert.assertTrue(diagnosticLine.contains("browserRebind=true"));
    }
}
