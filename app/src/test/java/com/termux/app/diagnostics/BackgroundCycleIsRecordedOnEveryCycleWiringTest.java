package com.termux.app.diagnostics;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class BackgroundCycleIsRecordedOnEveryCycleWiringTest {

    private static final String SESSION_CLIENT_PATH =
        "src/main/java/com/termux/app/terminal/TermuxTerminalSessionActivityClient.java";

    private static final String REPORT_COLLECTOR_PATH =
        "src/main/java/com/termux/app/diagnostics/DiagnosticsReportCollector.java";

    private String readModuleSource(String relativePath) throws IOException {
        Path moduleRelative = Paths.get(relativePath);
        if (Files.exists(moduleRelative)) {
            return new String(Files.readAllBytes(moduleRelative), StandardCharsets.UTF_8);
        }
        return new String(Files.readAllBytes(Paths.get("app").resolve(relativePath)),
            StandardCharsets.UTF_8);
    }

    private String methodBody(String source, String signature) {
        int start = source.indexOf(signature);
        Assert.assertTrue("method not found: " + signature, start >= 0);
        int end = source.indexOf("\n    }", start);
        Assert.assertTrue("method end not found: " + signature, end > start);
        return source.substring(start, end);
    }

    @Test
    public void everyBackgroundCycleIsRecordedBeforeAnythingCanReturnEarly() throws IOException {
        String body = methodBody(readModuleSource(SESSION_CLIENT_PATH),
            "private void refreshDisplayedSessionsForCallToUser() {");

        int recordIndex = body.indexOf("BackgroundCycleIntervalRecorderHolder.getInstance().recordCycle(");
        Assert.assertTrue("a cycle that returns early still ran, and not recording it would understate"
            + " the cycle count and hide a gap: " + body, recordIndex >= 0);

        int firstReturnIndex = body.indexOf("return;");
        Assert.assertTrue("the recording must precede every early return: " + body,
            firstReturnIndex < 0 || recordIndex < firstReturnIndex);
    }

    @Test
    public void theRecordedCycleCarriesTheScheduledIntervalAndTheActivityVisibility() throws IOException {
        String body = methodBody(readModuleSource(SESSION_CLIENT_PATH),
            "private void refreshDisplayedSessionsForCallToUser() {");

        Assert.assertTrue("without the scheduled interval a measured gap cannot be judged late: " + body,
            body.contains("displayedSessionCallScanIntervalMillis()"));
        Assert.assertTrue("without the visibility a backgrounded freeze cannot be told from a foreground"
            + " stall: " + body, body.contains("mActivity.isVisible()"));
    }

    @Test
    public void theDiagnosticsReportCarriesTheRecordedCycles() throws IOException {
        String body = methodBody(readModuleSource(REPORT_COLLECTOR_PATH),
            "public DiagnosticsReport collect(");

        Assert.assertTrue("the measurement is worthless unless the shared report the owner captures"
                + " carries it: " + body,
            body.contains("DiagnosticsBackgroundCycle.of(BackgroundCycleIntervalRecorderHolder.getInstance())"));
    }
}
