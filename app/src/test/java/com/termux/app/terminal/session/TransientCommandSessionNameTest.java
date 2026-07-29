package com.termux.app.terminal.session;

import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;

public class TransientCommandSessionNameTest {

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
    public void ordinarySessionNamesAreNotTransient() {
        Assert.assertFalse(TransientCommandSessionName.isTransient("work-session"));
        Assert.assertFalse(TransientCommandSessionName.isTransient("reset work-session"));
        Assert.assertFalse(TransientCommandSessionName.isTransient(null));
        Assert.assertFalse(TransientCommandSessionName.isTransient(""));
    }

    @Test
    public void finishedTransientSessionIsRemovedInsteadOfReconnectedEvenWhenAConnectTemplateIsConfigured() {
        FinishedSessionEnterAction action = FinishedSessionEnterAction.decide(
            TransientCommandSessionName.forResetOfSession("work-session"),
            "autossh -M 0 {name}", Collections.emptySet());

        Assert.assertEquals(FinishedSessionEnterAction.Kind.REMOVE, action.getKind());
        Assert.assertFalse(action.isReconnect());
        Assert.assertNull(action.getCommand());
    }

    @Test
    public void finishedOrdinarySessionIsStillReconnectedWhenAConnectTemplateIsConfigured() {
        FinishedSessionEnterAction action = FinishedSessionEnterAction.decide(
            "work-session", "autossh -M 0 {name}", Collections.emptySet());

        Assert.assertTrue(action.isReconnect());
    }
}
