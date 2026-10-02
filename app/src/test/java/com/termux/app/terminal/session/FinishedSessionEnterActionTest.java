package com.termux.app.terminal.session;

import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;

public class FinishedSessionEnterActionTest {

    private static final String GITHUB_SESSION_NAME =
        "https://github.com/HiromiShikata/termux-app/issues/2024";

    private static final String OTHER_GITHUB_SESSION_NAME =
        "https://github.com/HiromiShikata/termux-app/issues/9999";

    @Test
    public void decideRemovesGithubSessionAbsentFromPublishedListWhenListFetchedAndRemovalEnabled() {
        FinishedSessionEnterAction action = FinishedSessionEnterAction.decide(
            GITHUB_SESSION_NAME, "ssh {name}", Collections.emptySet(),
            true, Collections.singleton(OTHER_GITHUB_SESSION_NAME), Collections.emptySet(), true);

        Assert.assertFalse(action.isReconnect());
        Assert.assertEquals(FinishedSessionEnterAction.Kind.REMOVE, action.getKind());
        Assert.assertNull(action.getCommand());
    }

    @Test
    public void decideReconnectsGithubSessionPresentInPublishedList() {
        FinishedSessionEnterAction action = FinishedSessionEnterAction.decide(
            GITHUB_SESSION_NAME, "ssh {name}", Collections.emptySet(),
            true, Collections.singleton(GITHUB_SESSION_NAME), Collections.emptySet(), true);

        Assert.assertTrue(action.isReconnect());
        Assert.assertEquals(FinishedSessionEnterAction.Kind.RECONNECT, action.getKind());
        Assert.assertEquals(GITHUB_SESSION_NAME, action.getSessionName());
    }

    @Test
    public void decideReconnectsGithubSessionWhenPublishedListNeverFetched() {
        FinishedSessionEnterAction action = FinishedSessionEnterAction.decide(
            GITHUB_SESSION_NAME, "ssh {name}", Collections.emptySet(),
            false, Collections.emptySet(), Collections.emptySet(), true);

        Assert.assertTrue(action.isReconnect());
        Assert.assertEquals(GITHUB_SESSION_NAME, action.getSessionName());
    }

    @Test
    public void decideReconnectsGithubSessionAbsentFromListWhenRemovalPreferenceDisabled() {
        FinishedSessionEnterAction action = FinishedSessionEnterAction.decide(
            GITHUB_SESSION_NAME, "ssh {name}", Collections.emptySet(),
            true, Collections.emptySet(), Collections.emptySet(), false);

        Assert.assertTrue(action.isReconnect());
        Assert.assertEquals(GITHUB_SESSION_NAME, action.getSessionName());
    }

    @Test
    public void decideReconnectsNonGithubSessionRegardlessOfPublishedListState() {
        FinishedSessionEnterAction action = FinishedSessionEnterAction.decide(
            "myhost", "ssh {name}", Collections.emptySet(),
            true, Collections.emptySet(), Collections.emptySet(), true);

        Assert.assertTrue(action.isReconnect());
        Assert.assertEquals("myhost", action.getSessionName());
    }

    @Test
    public void decideRemovesGithubSessionAbsentFromListAndUserRemoved() {
        FinishedSessionEnterAction action = FinishedSessionEnterAction.decide(
            GITHUB_SESSION_NAME, "ssh {name}", Collections.singleton(GITHUB_SESSION_NAME),
            true, Collections.singleton(OTHER_GITHUB_SESSION_NAME), Collections.emptySet(), true);

        Assert.assertFalse(action.isReconnect());
        Assert.assertEquals(FinishedSessionEnterAction.Kind.REMOVE, action.getKind());
        Assert.assertNull(action.getCommand());
    }

    @Test
    public void decideRemovesGithubSessionPresentInListWhenAutosshTemplateBlank() {
        FinishedSessionEnterAction action = FinishedSessionEnterAction.decide(
            GITHUB_SESSION_NAME, "   ", Collections.emptySet(),
            true, Collections.singleton(GITHUB_SESSION_NAME), Collections.emptySet(), true);

        Assert.assertFalse(action.isReconnect());
        Assert.assertEquals(FinishedSessionEnterAction.Kind.REMOVE, action.getKind());
        Assert.assertNull(action.getCommand());
    }

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
        Assert.assertEquals("ssh -o ServerAliveInterval=30 -o ServerAliveCountMax=3 -o TCPKeepAlive=yes -o ConnectTimeout=10 'myhost'", action.getCommand());
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
        Assert.assertEquals("ssh -o ServerAliveInterval=30 -o ServerAliveCountMax=3 -o TCPKeepAlive=yes -o ConnectTimeout=10 'a'\\''b'", action.getCommand());
    }

    @Test
    public void decideTrimsAutosshTemplateBeforeBuildingReconnectCommand() {
        FinishedSessionEnterAction action = FinishedSessionEnterAction.decide("myhost", "  ssh {name}  ");

        Assert.assertTrue(action.isReconnect());
        Assert.assertEquals("ssh -o ServerAliveInterval=30 -o ServerAliveCountMax=3 -o TCPKeepAlive=yes -o ConnectTimeout=10 'myhost'", action.getCommand());
    }
}
