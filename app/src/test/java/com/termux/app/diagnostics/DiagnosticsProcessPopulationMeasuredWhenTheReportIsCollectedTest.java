package com.termux.app.diagnostics;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DiagnosticsProcessPopulationMeasuredWhenTheReportIsCollectedTest {

    private static final String COLLECTOR_RELATIVE_PATH =
        "src/main/java/com/termux/app/diagnostics/DiagnosticsReportCollector.java";

    private String readCollectorSource() throws IOException {
        Path moduleRelative = Paths.get(COLLECTOR_RELATIVE_PATH);
        if (Files.exists(moduleRelative)) {
            return new String(Files.readAllBytes(moduleRelative), StandardCharsets.UTF_8);
        }
        Path repoRelative = Paths.get("app").resolve(COLLECTOR_RELATIVE_PATH);
        return new String(Files.readAllBytes(repoRelative), StandardCharsets.UTF_8);
    }

    @Test
    public void theProcessPopulationIsReadWhileTheReportIsBeingCollected() throws IOException {
        String source = readCollectorSource();

        Assert.assertTrue("the report states how many processes the app is running so the owner can"
                + " compare that number against the ceiling Android enforces. A number carried over"
                + " from an earlier moment describes a population that no longer exists, and on a"
                + " report taken soon after start-up it states the population from before any session"
                + " had started. The collector therefore has to read the population itself.",
            source.contains("new AppProcessPopulationReader("));
        Assert.assertTrue("reading the population means asking the process table for it at collection"
                + " time rather than sampling a value something else recorded earlier.",
            source.contains(".read()"));
    }

    @Test
    public void theReportDoesNotCarryOverAPopulationRecordedByAnEarlierBackgroundCycle()
        throws IOException {
        String source = readCollectorSource();

        Assert.assertFalse("a population sampled from a holder is whatever the last background cycle"
                + " recorded, at whatever time that cycle ran, so the report would state a number that"
                + " does not belong to the moment the report describes.",
            source.contains("AppProcessPopulationHolder"));
    }
}
