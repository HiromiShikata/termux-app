package com.termux.app.terminal.tts;

import android.speech.tts.TextToSpeech;

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Binding to the platform speech service is asynchronous and the engine decides when it completes, so
 * the activity can be torn down between asking for the engine and the engine answering. The
 * application process was killed on the continuous integration emulator by the resulting
 * RejectedExecutionException, which the platform delivers on the main thread where nothing catches it.
 */
public class TtsEngineFinishingAfterShutdownTest {

    private static final int AMPLE_TIME_FOR_THE_WORKER_TO_FINISH_SECONDS = 5;

    @Test
    public void theEngineFinishingAfterTheSynthesizerWasShutDownReachesNoStoppedWorker() {
        ExecutorService engineWorkExecutor = Executors.newSingleThreadExecutor();
        AtomicBoolean theCallerWasToldTheEngineIsReady = new AtomicBoolean(false);
        AndroidTextToSpeechSynthesizer synthesizer =
            new AndroidTextToSpeechSynthesizer(null, engineWorkExecutor, Runnable::run);
        synthesizer.shutdown();

        synthesizer.onEngineInitialized(TextToSpeech.SUCCESS,
            () -> theCallerWasToldTheEngineIsReady.set(true));

        Assert.assertFalse("the synthesizer was shut down before the engine answered, so there is no"
                + " speech queue left to flush and telling the caller the engine is ready would hand"
                + " work to a worker that is gone",
            theCallerWasToldTheEngineIsReady.get());
    }

    @Test
    public void theEngineFinishingWhileTheSynthesizerIsAliveStillTellsTheCallerItIsReady()
            throws InterruptedException {
        ExecutorService engineWorkExecutor = Executors.newSingleThreadExecutor();
        AtomicBoolean theCallerWasToldTheEngineIsReady = new AtomicBoolean(false);
        AndroidTextToSpeechSynthesizer synthesizer =
            new AndroidTextToSpeechSynthesizer(null, engineWorkExecutor, Runnable::run);

        synthesizer.onEngineInitialized(TextToSpeech.SUCCESS,
            () -> theCallerWasToldTheEngineIsReady.set(true));

        engineWorkExecutor.shutdown();
        Assert.assertTrue("the worker must finish for this test to mean anything",
            engineWorkExecutor.awaitTermination(AMPLE_TIME_FOR_THE_WORKER_TO_FINISH_SECONDS,
                TimeUnit.SECONDS));
        Assert.assertTrue("speech queued before the engine was ready is flushed the moment the caller is"
                + " told it is ready, so a live synthesizer must still be told",
            theCallerWasToldTheEngineIsReady.get());
    }
}
