package com.termux.app.diagnostics;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DiagnosticsRequestTagControllerTest {

    private static final String TAG_BLOCK = "<diagnostics-report>please</diagnostics-report>";

    private final List<String> sessionKeysAskedForAReport = new ArrayList<>();

    private final DiagnosticsRequestTagController controller =
        new DiagnosticsRequestTagController(sessionKeysAskedForAReport::add);

    @Test
    public void aSessionThatPrintsTheTagIsSentItsOwnReport() {
        controller.onSessionTextChanged("session-a", "output before\n" + TAG_BLOCK + "\noutput after");

        Assert.assertEquals(Collections.singletonList("session-a"), sessionKeysAskedForAReport);
    }

    @Test
    public void anOccurrenceStillVisibleInTheTranscriptDoesNotAskAgain() {
        String transcript = "output\n" + TAG_BLOCK + "\nmore output";

        controller.onSessionTextChanged("session-a", transcript);
        controller.onSessionTextChanged("session-a", transcript + "\neven more output");

        Assert.assertEquals("the transcript keeps showing an occurrence that already produced a"
                + " report, and sending the report again on every scan would fill the session with"
                + " reports nobody asked for",
            Collections.singletonList("session-a"), sessionKeysAskedForAReport);
    }

    @Test
    public void aSecondRequestPrintedLaterAsksAgain() {
        controller.onSessionTextChanged("session-a", "output\n" + TAG_BLOCK);
        controller.onSessionTextChanged("session-a", "output\n" + TAG_BLOCK + "\nlater\n" + TAG_BLOCK);

        Assert.assertEquals(Arrays.asList("session-a", "session-a"), sessionKeysAskedForAReport);
    }

    @Test
    public void eachSessionIsTrackedOnItsOwn() {
        controller.onSessionTextChanged("session-a", TAG_BLOCK);
        controller.onSessionTextChanged("session-b", TAG_BLOCK);

        Assert.assertEquals(Arrays.asList("session-a", "session-b"), sessionKeysAskedForAReport);
    }

    @Test
    public void outputWithoutTheTagAsksForNothing() {
        controller.onSessionTextChanged("session-a", "a line mentioning diagnostics-report in prose");

        Assert.assertEquals(new ArrayList<String>(), sessionKeysAskedForAReport);
    }

    @Test
    public void anIncompleteTagBlockAsksForNothing() {
        controller.onSessionTextChanged("session-a", "<diagnostics-report>please");

        Assert.assertEquals("a half-written block is what a transcript shows while the tag is still"
                + " being printed, and acting on it would send a report for a request the session has"
                + " not finished making",
            new ArrayList<String>(), sessionKeysAskedForAReport);
    }

    @Test
    public void aTagBlockWithNoLabelStillAsksForAReport() {
        controller.onSessionTextChanged("session-a", "<diagnostics-report></diagnostics-report>");

        Assert.assertEquals(Collections.singletonList("session-a"), sessionKeysAskedForAReport);
    }

    @Test
    public void aForgottenSessionStartsCountingItsOccurrencesAgain() {
        controller.onSessionTextChanged("session-a", TAG_BLOCK);
        controller.forgetSession("session-a");
        controller.onSessionTextChanged("session-a", TAG_BLOCK);

        Assert.assertEquals(Arrays.asList("session-a", "session-a"), sessionKeysAskedForAReport);
    }

    @Test
    public void nothingIsSentWhileNoSenderIsAttached() {
        DiagnosticsRequestTagController controllerWithoutSender =
            new DiagnosticsRequestTagController(null);

        controllerWithoutSender.onSessionTextChanged("session-a", TAG_BLOCK);

        Assert.assertEquals(new ArrayList<String>(), sessionKeysAskedForAReport);
    }

    @Test
    public void aNullSessionKeyIsIgnored() {
        controller.onSessionTextChanged(null, TAG_BLOCK);

        Assert.assertEquals(new ArrayList<String>(), sessionKeysAskedForAReport);
    }
}
