package com.termux.app.terminal.session;

import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;

public class FinishedSessionEnterActionTest {

    @Test
    public void decideRemovesUserRemovedSessionEvenWhenAutosshTemplateConfigured() {
        FinishedSessionEnterAction action = FinishedSessionEnterAction.decide(
            "google logon", "ssh {name}", Collections.singleton("google logon"));

        Assert.assertFalse(action.isReconnect());
        Assert.assertEquals(FinishedSessionEnterAction.Kind.REMOVE, action.getKind());
        Assert.assertNull(action.getCommand());
    }

    @Test
    public void decideReconnectsSessionNotInUserRemovedSet() {
        FinishedSessionEnterAction action = FinishedSessionEnterAction.decide(
            "myhost", "ssh {name}", Collections.singleton("google logon"));

        Assert.assertTrue(action.isReconnect());
        Assert.assertEquals("myhost", action.getSessionName());
    }

    @Test
    public void decideReconnectsDefinitionBackedSessionWhenAutosshTemplateConfigured() {
        FinishedSessionEnterAction action = FinishedSessionEnterAction.decide("myhost", "ssh {name}");

        Assert.assertTrue(action.isReconnect());
        Assert.assertEquals(FinishedSessionEnterAction.Kind.RECONNECT, action.getKind());
        Assert.assertEquals("myhost", action.getSessionName());
        Assert.assertEquals("ssh -o ServerAliveInterval=30 -o ServerAliveCountMax=3 -o TCPKeepAlive=yes 'myhost'", action.getCommand());
    }

    @Test
    public void decideRemovesPlainSessionWhenAutosshTemplateBlank() {
        FinishedSessionEnterAction action = FinishedSessionEnterAction.decide("myhost", "   ");

        Assert.assertFalse(action.isReconnect());
        Assert.assertEquals(FinishedSessionEnterAction.Kind.REMOVE, action.getKind());
        Assert.assertNull(action.getCommand());
    }

    @Test
    public void decideRemovesPlainSessionWhenAutosshTemplateNull() {
        FinishedSessionEnterAction action = FinishedSessionEnterAction.decide("myhost", null);

        Assert.assertFalse(action.isReconnect());
        Assert.assertNull(action.getCommand());
    }

    @Test
    public void decideRemovesSessionWhenSessionNameNull() {
        FinishedSessionEnterAction action = FinishedSessionEnterAction.decide(null, "ssh {name}");

        Assert.assertFalse(action.isReconnect());
        Assert.assertNull(action.getCommand());
    }

    @Test
    public void decideRemovesSessionWhenSessionNameBlank() {
        FinishedSessionEnterAction action = FinishedSessionEnterAction.decide("   ", "ssh {name}");

        Assert.assertFalse(action.isReconnect());
        Assert.assertNull(action.getCommand());
    }

    @Test
    public void decideShellQuotesSessionNameWithSingleQuoteInReconnectCommand() {
        FinishedSessionEnterAction action = FinishedSessionEnterAction.decide("a'b", "ssh {name}");

        Assert.assertTrue(action.isReconnect());
        Assert.assertEquals("ssh -o ServerAliveInterval=30 -o ServerAliveCountMax=3 -o TCPKeepAlive=yes 'a'\\''b'", action.getCommand());
    }

    @Test
    public void decideTrimsAutosshTemplateBeforeBuildingReconnectCommand() {
        FinishedSessionEnterAction action = FinishedSessionEnterAction.decide("myhost", "  ssh {name}  ");

        Assert.assertTrue(action.isReconnect());
        Assert.assertEquals("ssh -o ServerAliveInterval=30 -o ServerAliveCountMax=3 -o TCPKeepAlive=yes 'myhost'", action.getCommand());
    }
}
