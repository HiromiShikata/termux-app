package com.termux.app.terminal;

import com.termux.app.diagnostics.DiagnosticsRequestTagController;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DiagnosticsRequestTagIsScannedForEverySessionTest {

    private static final String TAG_BLOCK = "<diagnostics-report>please</diagnostics-report>";

    private final List<String> sessionKeysAskedForAReport = new ArrayList<>();

    private final DiagnosticsRequestTagController diagnosticsRequestTagController =
        new DiagnosticsRequestTagController(sessionKeysAskedForAReport::add);

    private final BackgroundOutputTagScanner scanner =
        new BackgroundOutputTagScanner(null, null, diagnosticsRequestTagController);

    @Test
    public void theRequestIsDetectedWithoutTheCallToUserScanBeingRun() {
        scanner.scan("session-a", "output\n" + TAG_BLOCK, false);

        Assert.assertEquals("the request is answered for any session, whichever session the owner is"
                + " viewing and whether or not that session has a pending call, so it must not sit"
                + " behind the call-to-user scan gate",
            Collections.singletonList("session-a"), sessionKeysAskedForAReport);
    }

    @Test
    public void aSessionWithNoTranscriptAsksForNothing() {
        scanner.scan("session-a", null, true);

        Assert.assertEquals(new ArrayList<String>(), sessionKeysAskedForAReport);
    }
}
