package com.termux.app.terminal.tts;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * The engine initialization callback is delivered on the main thread. Applying a language there
 * makes the engine hand back its voice list, whose entries carry a serialized locale, and
 * deserializing that list stalled the owner's main thread for 963 ms.
 */
public class TtsEngineReadyPlannerTest {

    private static final boolean ENGINE_INITIALIZED = true;
    private static final boolean ENGINE_FAILED_TO_INITIALIZE = false;

    private final TtsEngineReadyPlanner planner = new TtsEngineReadyPlanner();
    private final List<String> record = new ArrayList<>();

    private final Executor runsImmediately = Runnable::run;

    private static final class DeferringExecutor implements Executor {
        private final List<Runnable> deferred = new ArrayList<>();

        @Override
        public void execute(Runnable command) {
            deferred.add(command);
        }

        void runEverythingDeferred() {
            List<Runnable> pending = new ArrayList<>(deferred);
            deferred.clear();
            for (Runnable command : pending) {
                command.run();
            }
        }
    }

    private Runnable recording(String entry) {
        return () -> record.add(entry);
    }

    @Test
    public void theDefaultLanguageIsNeverAppliedOnTheThreadTheEngineCallsBackOn() {
        DeferringExecutor engineWorkExecutor = new DeferringExecutor();

        planner.onEngineInitialized(ENGINE_INITIALIZED, engineWorkExecutor, runsImmediately,
            recording("language applied"), recording("ready"));

        Assert.assertEquals("applying the language makes the engine deserialize its whole voice list,"
                + " which took 963 ms on the owner's device, so it must never run on the thread the"
                + " engine calls back on. Actual: " + record,
            new ArrayList<String>(), record);
    }

    @Test
    public void theCallerIsToldTheEngineIsReadyOnlyAfterTheLanguageHasBeenApplied() {
        DeferringExecutor engineWorkExecutor = new DeferringExecutor();

        planner.onEngineInitialized(ENGINE_INITIALIZED, engineWorkExecutor, runsImmediately,
            recording("language applied"), recording("ready"));
        engineWorkExecutor.runEverythingDeferred();

        Assert.assertEquals("speech queued before the engine was ready is flushed the moment the caller"
                + " is told it is ready, so the language must already be applied by then. Actual: "
                + record,
            java.util.Arrays.asList("language applied", "ready"), record);
    }

    @Test
    public void theCallerIsToldTheEngineIsReadyOnTheThreadThatOwnsTheSpeechQueue() {
        DeferringExecutor engineWorkExecutor = new DeferringExecutor();
        DeferringExecutor speechQueueExecutor = new DeferringExecutor();

        planner.onEngineInitialized(ENGINE_INITIALIZED, engineWorkExecutor, speechQueueExecutor,
            recording("language applied"), recording("ready"));
        engineWorkExecutor.runEverythingDeferred();

        Assert.assertEquals("only the language application belongs on the worker. Actual: " + record,
            java.util.Collections.singletonList("language applied"), record);

        speechQueueExecutor.runEverythingDeferred();

        Assert.assertEquals("the pending speech queue is not thread safe, so the caller must be told the"
                + " engine is ready on the thread that owns it. Actual: " + record,
            java.util.Arrays.asList("language applied", "ready"), record);
    }

    @Test
    public void anEngineThatFailedToInitializeAppliesNoLanguageAndTellsNobodyItIsReady() {
        planner.onEngineInitialized(ENGINE_FAILED_TO_INITIALIZE, runsImmediately, runsImmediately,
            recording("language applied"), recording("ready"));

        Assert.assertEquals("an engine that failed to initialize can neither take a language nor speak,"
                + " so telling the caller it is ready would flush the queued speech into nothing."
                + " Actual: " + record,
            new ArrayList<String>(), record);
    }
}
