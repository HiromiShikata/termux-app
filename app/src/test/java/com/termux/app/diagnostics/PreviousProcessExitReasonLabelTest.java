package com.termux.app.diagnostics;

import org.junit.Assert;
import org.junit.Test;

public class PreviousProcessExitReasonLabelTest {

    @Test
    public void aCrashIsNamedAsACrashRatherThanAsACode() {
        Assert.assertEquals("the whole point of this section is that a reader can tell a crash from a"
                + " memory kill without looking a number up",
            "a crash in Java code", PreviousProcessExitReasonLabel.of(4));
    }

    @Test
    public void aMemoryKillIsNamedSoItIsNotMistakenForACrash() {
        Assert.assertEquals("a process the system reclaimed for memory is not a defect in the app, and"
                + " reading it as one sends the investigation to the wrong place",
            "the system reclaiming memory", PreviousProcessExitReasonLabel.of(3));
    }

    @Test
    public void anUnresponsiveMainThreadIsNamedAsSuch() {
        Assert.assertEquals("an unresponsive main thread is the symptom under investigation, so it has to"
                + " be recognisable on sight",
            "an unresponsive main thread", PreviousProcessExitReasonLabel.of(6));
    }

    @Test
    public void anUpdateOfTheAppIsNamedSoARoutineRestartIsNotReadAsAFault() {
        Assert.assertEquals("the app updates itself often, and every update ends the running process, so"
                + " that ending must not look like a failure",
            "the app being updated", PreviousProcessExitReasonLabel.of(16));
    }

    @Test
    public void excessiveResourceUseIsNamedBecauseItIsWhatTheProcessCountWouldTrigger() {
        Assert.assertEquals("this app holds one shell process per session, so a kill for resource use is"
                + " a live candidate and has to be distinguishable",
            "the system judging the app's resource use excessive", PreviousProcessExitReasonLabel.of(9));
    }

    @Test
    public void aCodeWithNoNameIsReportedAsThatCodeRatherThanAsSomethingElse() {
        String label = PreviousProcessExitReasonLabel.of(987);

        Assert.assertTrue("a code this app has no name for must be shown as the code it is, because"
                + " presenting it as any named reason would be a fabricated attribution. Actual label: "
                + label,
            label.contains("987"));
    }
}
