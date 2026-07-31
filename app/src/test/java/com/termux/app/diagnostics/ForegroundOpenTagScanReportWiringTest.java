package com.termux.app.diagnostics;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ForegroundOpenTagScanReportWiringTest {

    @Test
    public void theCollectorPutsTheRecordedForegroundScanCostIntoTheReport() throws IOException {
        String source = new String(Files.readAllBytes(
                Paths.get("src/main/java/com/termux/app/diagnostics/DiagnosticsReportCollector.java")),
            StandardCharsets.UTF_8);

        Assert.assertTrue("A cost recorded into a counter that the collector never reads would leave the report"
                + " showing zero forever while the work is really being done: " + source,
            source.contains("DiagnosticsWorkCostLine.of(ForegroundOpenTagScanCostCounterHolder.getInstance())"));
    }
}
