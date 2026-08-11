package com.termux.app.terminal.tts;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

public class TtsEngineCallsRunOffTheCallingThreadTest {

    private static final class RecordingEngineWorkExecutor extends AbstractExecutorService {

        private final List<Runnable> mAcceptedWork = new ArrayList<>();

        private boolean mShutDown;

        @Override
        public void execute(Runnable command) {
            if (mShutDown) {
                throw new RejectedExecutionException("the engine work executor is shut down");
            }
            mAcceptedWork.add(command);
        }

        @Override
        public void shutdown() {
            mShutDown = true;
        }

        @Override
        public List<Runnable> shutdownNow() {
            mShutDown = true;
            return new ArrayList<>();
        }

        @Override
        public boolean isShutdown() {
            return mShutDown;
        }

        @Override
        public boolean isTerminated() {
            return mShutDown;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) {
            return mShutDown;
        }

        int acceptedWorkCount() {
            return mAcceptedWork.size();
        }
    }

    private static AndroidTextToSpeechSynthesizer synthesizerUsing(
            RecordingEngineWorkExecutor engineWorkExecutor) {
        return new AndroidTextToSpeechSynthesizer(null, engineWorkExecutor, Runnable::run);
    }

    @Test
    public void stoppingSpeechHandsTheEngineCallToTheWorkerInsteadOfMakingItOnTheCallingThread() {
        RecordingEngineWorkExecutor engineWorkExecutor = new RecordingEngineWorkExecutor();
        AndroidTextToSpeechSynthesizer synthesizer = synthesizerUsing(engineWorkExecutor);

        synthesizer.stop();

        Assert.assertEquals("stopping the engine is a binder call into the platform speech service and it"
                + " blocked the owner's main thread for 749 ms, so it must be handed to the engine work"
                + " thread rather than made on the thread that asked for the stop",
            1, engineWorkExecutor.acceptedWorkCount());
    }

    @Test
    public void speakingHandsTheEngineCallToTheSameWorkerSoAStopCannotOvertakeSpeech() {
        RecordingEngineWorkExecutor engineWorkExecutor = new RecordingEngineWorkExecutor();
        AndroidTextToSpeechSynthesizer synthesizer = synthesizerUsing(engineWorkExecutor);

        synthesizer.speak("hello", 0, "utterance");
        synthesizer.stop();

        Assert.assertEquals("speaking and stopping must reach the engine through the same single-threaded"
                + " worker, because a stop that overtook the speech it was meant to cancel would leave the"
                + " app speaking after the owner silenced it",
            2, engineWorkExecutor.acceptedWorkCount());
    }

    @Test
    public void shuttingTheEngineDownIsHandedToTheWorkerBeforeTheWorkerIsStopped() {
        RecordingEngineWorkExecutor engineWorkExecutor = new RecordingEngineWorkExecutor();
        AndroidTextToSpeechSynthesizer synthesizer = synthesizerUsing(engineWorkExecutor);

        synthesizer.shutdown();

        Assert.assertEquals("shutting the engine down is itself a call into the platform speech service, and"
                + " it must be queued behind the speech already handed to the worker rather than run on the"
                + " calling thread ahead of it",
            1, engineWorkExecutor.acceptedWorkCount());
        Assert.assertTrue("the worker must still be stopped, so a synthesizer the activity replaces does not"
                + " leave a thread behind",
            engineWorkExecutor.isShutdown());
    }

    @Test
    public void noEngineWorkIsScheduledOnceTheSynthesizerHasBeenShutDown() {
        RecordingEngineWorkExecutor engineWorkExecutor = new RecordingEngineWorkExecutor();
        AndroidTextToSpeechSynthesizer synthesizer = synthesizerUsing(engineWorkExecutor);
        synthesizer.shutdown();
        int acceptedWorkCountAfterShutdown = engineWorkExecutor.acceptedWorkCount();

        synthesizer.stop();
        synthesizer.speak("hello", 0, "utterance");

        Assert.assertEquals("a shut-down executor rejects work, so a synthesizer that keeps handing engine"
                + " calls to it after shutdown crashes the app instead of doing nothing",
            acceptedWorkCountAfterShutdown, engineWorkExecutor.acceptedWorkCount());
    }
}
