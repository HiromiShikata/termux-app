package com.termux.app.terminal.io;

import org.junit.Assert;
import org.junit.Test;

public class BareEnterWriteDecisionTest {

    @Test
    public void theFirstBareEnterOnASessionIsAlwaysWritten() {
        Assert.assertTrue("the owner must be able to confirm a prompt, and the extra keys row carries no"
                + " enter key, so the send button is the only way to do it",
            BareEnterWriteDecision.shouldWrite(null, null));
    }

    @Test
    public void aBareEnterIsNotRepeatedWhileTheSessionHasProducedNothing() {
        Assert.assertFalse("a session that has produced no output at all since the last bare enter is not"
                + " consuming input, so a further enter can only be delivered late as a blank line",
            BareEnterWriteDecision.shouldWrite(1000L, null));
    }

    @Test
    public void aBareEnterIsNotRepeatedWhileTheOnlyOutputPredatesIt() {
        Assert.assertFalse("output that arrived before the last bare enter says nothing about whether the"
                + " session is consuming input now",
            BareEnterWriteDecision.shouldWrite(1000L, 900L));
    }

    @Test
    public void aBareEnterIsWrittenAgainOnceTheSessionHasAnswered() {
        Assert.assertTrue("output produced after the last bare enter shows the session is consuming input,"
                + " so the owner must be able to confirm one prompt after another",
            BareEnterWriteDecision.shouldWrite(1000L, 1001L));
    }

    @Test
    public void outputAtTheSameMillisecondAsTheBareEnterDoesNotCountAsAnAnswer() {
        Assert.assertFalse("output recorded in the same millisecond cannot be shown to be a response to"
                + " the bare enter rather than something that raced it",
            BareEnterWriteDecision.shouldWrite(1000L, 1000L));
    }
}
