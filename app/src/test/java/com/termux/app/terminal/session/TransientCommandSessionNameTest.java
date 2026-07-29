package com.termux.app.terminal.session;

import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;

public class TransientCommandSessionNameTest {

    private static final String CONNECT_TEMPLATE = "autossh -M 0 {name}";

    @Test
    public void resetSessionNameIsDistinctFromTheSessionBeingReset() {
        Assert.assertEquals("[reset] work-session",
            TransientCommandSessionName.forResetOfSession("work-session"));
    }

    @Test
    public void unnamedSessionHasNoResetSessionName() {
        Assert.assertNull(TransientCommandSessionName.forResetOfSession(null));
        Assert.assertNull(TransientCommandSessionName.forResetOfSession(""));
    }

    @Test
    public void resetSessionNamesAreRecognizedAsTransient() {
        Assert.assertTrue(TransientCommandSessionName.isTransient(
            TransientCommandSessionName.forResetOfSession("work-session")));
    }

    @Test
    public void killSessionNameIsDistinctFromTheSessionBeingKilled() {
        Assert.assertEquals("[kill] work-session",
            TransientCommandSessionName.forKillOfSession("work-session"));
    }

    @Test
    public void unnamedSessionHasNoKillSessionName() {
        Assert.assertNull(TransientCommandSessionName.forKillOfSession(null));
        Assert.assertNull(TransientCommandSessionName.forKillOfSession(""));
    }

    @Test
    public void killAndResetOfTheSameSessionAreDistinctSessions() {
        Assert.assertNotEquals(TransientCommandSessionName.forKillOfSession("work-session"),
            TransientCommandSessionName.forResetOfSession("work-session"));
    }

    @Test
    public void killSessionNamesAreRecognizedAsTransient() {
        Assert.assertTrue(TransientCommandSessionName.isTransient(
            TransientCommandSessionName.forKillOfSession("work-session")));
    }

    @Test
    public void ordinarySessionNamesAreNotTransient() {
        Assert.assertFalse(TransientCommandSessionName.isTransient("work-session"));
        Assert.assertFalse(TransientCommandSessionName.isTransient("reset work-session"));
        Assert.assertFalse(TransientCommandSessionName.isTransient("kill work-session"));
        Assert.assertFalse(TransientCommandSessionName.isTransient(null));
        Assert.assertFalse(TransientCommandSessionName.isTransient(""));
    }

    @Test
    public void finishedResetSessionIsRemovedInsteadOfReconnectedEvenWhenAConnectTemplateIsConfigured() {
        FinishedSessionEnterAction action = FinishedSessionEnterAction.decide(
            TransientCommandSessionName.forResetOfSession("work-session"),
            CONNECT_TEMPLATE, Collections.emptySet());

        Assert.assertEquals(FinishedSessionEnterAction.Kind.REMOVE, action.getKind());
        Assert.assertFalse(action.isReconnect());
        Assert.assertNull(action.getCommand());
    }

    @Test
    public void finishedKillSessionIsRemovedInsteadOfReconnectedEvenWhenAConnectTemplateIsConfigured() {
        FinishedSessionEnterAction action = FinishedSessionEnterAction.decide(
            TransientCommandSessionName.forKillOfSession("work-session"),
            CONNECT_TEMPLATE, Collections.emptySet());

        Assert.assertEquals(FinishedSessionEnterAction.Kind.REMOVE, action.getKind());
        Assert.assertFalse(action.isReconnect());
        Assert.assertNull(action.getCommand());
    }

    @Test
    public void finishedOrdinarySessionIsStillReconnectedWhenAConnectTemplateIsConfigured() {
        FinishedSessionEnterAction action = FinishedSessionEnterAction.decide(
            "work-session", CONNECT_TEMPLATE, Collections.emptySet());

        Assert.assertTrue(action.isReconnect());
    }
}
