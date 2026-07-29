package com.termux.app.terminal;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ForcedStatuslineRescanBatchWiringTest {

    private static final String CLIENT_RELATIVE_PATH =
        "src/main/java/com/termux/app/terminal/TermuxTerminalSessionActivityClient.java";

    private String readClientSource() throws IOException {
        Path moduleRelative = Paths.get(CLIENT_RELATIVE_PATH);
        if (Files.exists(moduleRelative)) {
            return new String(Files.readAllBytes(moduleRelative), StandardCharsets.UTF_8);
        }
        Path repoRelative = Paths.get("app").resolve(CLIENT_RELATIVE_PATH);
        return new String(Files.readAllBytes(repoRelative), StandardCharsets.UTF_8);
    }

    private String methodBody(String source, String signature) {
        int methodIndex = source.indexOf(signature);
        Assert.assertTrue("method not found: " + signature, methodIndex >= 0);
        int methodEnd = source.indexOf("\n    }", methodIndex);
        Assert.assertTrue(methodEnd > methodIndex);
        return source.substring(methodIndex, methodEnd);
    }

    @Test
    public void forcedRescanReadsOnlyTheFirstBatchOfTranscriptsInTheCurrentMainThreadPass()
            throws IOException {
        String body = methodBody(readClientSource(),
            "private void repopulateStatuslineTimesForAllSessions(boolean forceRescan) {");
        Assert.assertTrue("a forced rescan reads every visible session's whole transcript regardless of "
                + "the skip-gate, so it must narrow the current main-thread pass to the first batch "
                + "instead of materializing the whole set at once",
            body.contains("firstForcedRescanBatchAfterDeferringTheRest("));
        Assert.assertTrue("the gated tick must stay a single pass, so the narrowing must apply only to "
                + "the forced variant",
            body.contains("if (forceRescan) {"));
    }

    @Test
    public void forcedRescanSpacesEveryLaterBatchTheWayTheDisplayedRefreshDoes() throws IOException {
        String body = methodBody(readClientSource(),
            "private Set<String> firstForcedRescanBatchAfterDeferringTheRest(");
        Assert.assertTrue("the forced rescan must chunk the transcript reads with the same batch size the "
                + "displayed refresh applies",
            body.contains("StaggeredStatuslineRescanBatchPlanner.plan(")
                && body.contains("STAGGERED_STATUSLINE_RESCAN_BATCH_SIZE"));
        Assert.assertTrue("every batch after the first must be posted to the main-thread handler a further "
                + "spacing interval out, so no pass reads more than one batch of transcripts",
            body.contains("mMainThreadHandler.postDelayed(")
                && body.contains("STAGGERED_RECONNECT_INTERVAL_MILLIS"));
        Assert.assertTrue("the deferred batches must go through the same gate-bypassing rescan body",
            body.contains("repopulateStatuslineTimesForSessionNames("));
    }

    @Test
    public void everyForcedRescanEntryPointIncludingTheRetryLadderGoesThroughTheBatchedPath()
            throws IOException {
        String source = readClientSource();
        String combinedRefreshBody = methodBody(source,
            "public void reconnectDeadDefinitionBackedSessionsThenForceRescanStatusline() {");
        String retryBody = methodBody(source,
            "private final class PostReconnectStatuslineRescanRetry");
        Assert.assertTrue("the reload and periodic reconnect entry point must force the rescan through the "
                + "batched path",
            combinedRefreshBody.contains("repopulateStatuslineTimesForAllSessions(true)"));
        Assert.assertTrue("each rung of the post-reconnect retry ladder must force the rescan through the "
                + "same batched path, because the ladder repeats the whole forced pass five more times "
                + "within twelve seconds",
            retryBody.contains("repopulateStatuslineTimesForAllSessions(true)"));
    }
}
