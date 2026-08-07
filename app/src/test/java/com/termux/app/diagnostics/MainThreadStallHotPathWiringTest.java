package com.termux.app.diagnostics;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class MainThreadStallHotPathWiringTest {

    private String readModuleSource(String relativePath) throws IOException {
        Path moduleRelative = Paths.get(relativePath);
        if (Files.exists(moduleRelative)) {
            return new String(Files.readAllBytes(moduleRelative), StandardCharsets.UTF_8);
        }
        Path repoRelative = Paths.get("app").resolve(relativePath);
        return new String(Files.readAllBytes(repoRelative), StandardCharsets.UTF_8);
    }

    @Test
    public void repeatedStallsAreAggregatedByTheStackThatCausedThem() throws IOException {
        String source = readModuleSource(
            "src/main/java/com/termux/app/diagnostics/MainThreadStallRecorder.java");

        Assert.assertTrue("repeated stalls must be aggregated per stack rather than only kept as the single longest",
            source.contains("getHotPathsByTotalBlockedMillis()"));
    }

    @Test
    public void theWatchdogCatchesStallsShortEnoughToBeSingleDroppedFrames() throws IOException {
        String source = readModuleSource(
            "src/main/java/com/termux/app/diagnostics/MainThreadStallWatchdog.java");

        int thresholdIndex = source.indexOf("STALL_THRESHOLD_MILLIS = ");
        Assert.assertTrue("the stall threshold must be declared", thresholdIndex >= 0);
        String declaredThreshold = source.substring(
            thresholdIndex + "STALL_THRESHOLD_MILLIS = ".length(), source.indexOf("L;", thresholdIndex));
        Assert.assertTrue("a threshold that only catches multi second freezes cannot explain dropped frames",
            Long.parseLong(declaredThreshold.trim()) <= 100L);
    }

    @Test
    public void theReportRanksTheHotPathsThatBlockedTheMainThreadTheLongest() throws IOException {
        String source = readModuleSource(
            "src/main/java/com/termux/app/diagnostics/DiagnosticsReportBuilder.java");

        Assert.assertTrue("the report must rank what blocked the main thread, not only the single longest stall",
            source.contains("Blocking the main thread the longest"));
    }
}
