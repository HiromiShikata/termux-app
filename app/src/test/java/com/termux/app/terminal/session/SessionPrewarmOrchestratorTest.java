package com.termux.app.terminal.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import androidx.annotation.NonNull;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SessionPrewarmOrchestratorTest {

    private static final class RecordingWarmAction implements SessionPrewarmOrchestrator.SessionWarmAction {
        final List<String> warmedSessionHandles = new ArrayList<>();

        @Override
        public void warmSession(@NonNull String sessionHandle) {
            warmedSessionHandles.add(sessionHandle);
        }
    }

    private static final class ImmediateBackgroundExecutor implements SessionPrewarmOrchestrator.BackgroundExecutor {
        boolean executedOffMainThread;

        @Override
        public void executeInBackground(@NonNull Runnable task) {
            executedOffMainThread = true;
            task.run();
        }
    }

    private static final class ImmediateMainThreadPoster implements SessionPrewarmOrchestrator.MainThreadPoster {
        int postCount;

        @Override
        public void postToMainThread(@NonNull Runnable task) {
            postCount++;
            task.run();
        }
    }

    @Test
    public void warmsEveryKnownSessionExactlyOnce() {
        RecordingWarmAction warmAction = new RecordingWarmAction();
        SessionPrewarmOrchestrator orchestrator = new SessionPrewarmOrchestrator(
            warmAction, new ImmediateBackgroundExecutor(), new ImmediateMainThreadPoster());

        orchestrator.prewarmSessions(Arrays.asList("handleA", "handleB", "handleC"));

        assertEquals(Arrays.asList("handleA", "handleB", "handleC"), warmAction.warmedSessionHandles);
    }

    @Test
    public void warmingRunsOffTheMainThread() {
        ImmediateBackgroundExecutor backgroundExecutor = new ImmediateBackgroundExecutor();
        SessionPrewarmOrchestrator orchestrator = new SessionPrewarmOrchestrator(
            new RecordingWarmAction(), backgroundExecutor, new ImmediateMainThreadPoster());

        orchestrator.prewarmSessions(Collections.singletonList("handleA"));

        assertTrue(backgroundExecutor.executedOffMainThread);
    }

    @Test
    public void eachWarmActionIsPostedToTheMainThread() {
        ImmediateMainThreadPoster mainThreadPoster = new ImmediateMainThreadPoster();
        SessionPrewarmOrchestrator orchestrator = new SessionPrewarmOrchestrator(
            new RecordingWarmAction(), new ImmediateBackgroundExecutor(), mainThreadPoster);

        orchestrator.prewarmSessions(Arrays.asList("handleA", "handleB"));

        assertEquals(2, mainThreadPoster.postCount);
    }

    @Test
    public void alreadyWarmedSessionsAreNotWarmedAgainOnSubsequentCalls() {
        RecordingWarmAction warmAction = new RecordingWarmAction();
        SessionPrewarmOrchestrator orchestrator = new SessionPrewarmOrchestrator(
            warmAction, new ImmediateBackgroundExecutor(), new ImmediateMainThreadPoster());

        orchestrator.prewarmSessions(Arrays.asList("handleA", "handleB"));
        orchestrator.prewarmSessions(Arrays.asList("handleA", "handleB", "handleC"));

        assertEquals(Arrays.asList("handleA", "handleB", "handleC"), warmAction.warmedSessionHandles);
    }

    @Test
    public void duplicateHandlesInOneCallAreWarmedOnlyOnce() {
        RecordingWarmAction warmAction = new RecordingWarmAction();
        SessionPrewarmOrchestrator orchestrator = new SessionPrewarmOrchestrator(
            warmAction, new ImmediateBackgroundExecutor(), new ImmediateMainThreadPoster());

        orchestrator.prewarmSessions(Arrays.asList("handleA", "handleA", "handleB"));

        assertEquals(Arrays.asList("handleA", "handleB"), warmAction.warmedSessionHandles);
    }

    @Test
    public void nullHandlesAreSkipped() {
        RecordingWarmAction warmAction = new RecordingWarmAction();
        SessionPrewarmOrchestrator orchestrator = new SessionPrewarmOrchestrator(
            warmAction, new ImmediateBackgroundExecutor(), new ImmediateMainThreadPoster());

        orchestrator.prewarmSessions(Arrays.asList("handleA", null, "handleB"));

        assertEquals(Arrays.asList("handleA", "handleB"), warmAction.warmedSessionHandles);
    }

    @Test
    public void emptySessionListDoesNotEngageBackgroundExecutor() {
        ImmediateBackgroundExecutor backgroundExecutor = new ImmediateBackgroundExecutor();
        SessionPrewarmOrchestrator orchestrator = new SessionPrewarmOrchestrator(
            new RecordingWarmAction(), backgroundExecutor, new ImmediateMainThreadPoster());

        orchestrator.prewarmSessions(Collections.emptyList());

        assertTrue(!backgroundExecutor.executedOffMainThread);
    }
}
