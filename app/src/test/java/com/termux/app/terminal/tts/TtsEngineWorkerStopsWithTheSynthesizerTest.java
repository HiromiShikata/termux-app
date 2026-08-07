package com.termux.app.terminal.tts;

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The activity builds a new text-to-speech manager, and with it a new synthesizer, every time it is
 * recreated. The worker that keeps the language application off the main thread is a pooled thread
 * that stays alive once started, so a synthesizer that does not stop its own worker leaves one
 * thread behind on every recreation.
 */
public class TtsEngineWorkerStopsWithTheSynthesizerTest {

    @Test
    public void shuttingTheSynthesizerDownAlsoStopsTheWorkerItStarted() {
        ExecutorService engineWorkExecutor = Executors.newSingleThreadExecutor();
        AndroidTextToSpeechSynthesizer synthesizer =
            new AndroidTextToSpeechSynthesizer(null, engineWorkExecutor, Runnable::run);

        synthesizer.shutdown();

        Assert.assertTrue("the activity creates a new synthesizer every time it is recreated, so a worker"
                + " that outlives the synthesizer that started it leaves a thread behind on every"
                + " recreation",
            engineWorkExecutor.isShutdown());
    }
}
