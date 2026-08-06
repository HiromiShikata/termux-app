package com.termux.terminal;

import org.junit.Assert;
import org.junit.Test;

public class TerminalInputDeliveryTest {

    @Test
    public void inputReachesTheRemoteShellClientWhileItHoldsTheTerminalInRawMode() {
        Assert.assertTrue(TerminalInputDelivery.reachesTheProgramReadingTheTerminal(true, false));
    }

    @Test
    public void inputDoesNotReachARemoteShellClientThatHasLeftTheTerminalInCanonicalMode() {
        Assert.assertFalse(TerminalInputDelivery.reachesTheProgramReadingTheTerminal(true, true));
    }

    @Test
    public void inputToALocalShellReachesItEvenThoughTheTerminalIsInCanonicalMode() {
        Assert.assertTrue(TerminalInputDelivery.reachesTheProgramReadingTheTerminal(false, true));
    }

    @Test
    public void inputToALocalFullScreenProgramReachesItInRawMode() {
        Assert.assertTrue(TerminalInputDelivery.reachesTheProgramReadingTheTerminal(false, false));
    }

    @Test
    public void theUndeliverableReasonNamesTheDetachedRemoteShellClient() {
        Assert.assertEquals("the remote shell client is not attached to this terminal",
            TerminalInputDelivery.REMOTE_SHELL_CLIENT_DETACHED_REASON);
    }
}
