package com.termux.app.terminal.tts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.AbstractExecutorService;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

@RunWith(RobolectricTestRunner.class)
public class AndroidTextToSpeechSynthesizerTest {
    @Test
    public void doneCallbackReportsSpokenOnceOnSpeechExecutor() throws Exception {
        ExecutorService engine = new DirectExecutorService();
        RecordingExecutor speech = new RecordingExecutor();
        AndroidTextToSpeechSynthesizer synthesizer = new AndroidTextToSpeechSynthesizer(
            RuntimeEnvironment.getApplication(), engine, speech);
        RecordingOutcome outcome = new RecordingOutcome();
        synthesizer.initialize(() -> { });
        synthesizer.speak("hello", TextToSpeech.QUEUE_FLUSH, "utterance", outcome);
        String id = registeredId(synthesizer);
        UtteranceProgressListener listener = progressListener(synthesizer);
        listener.onDone(id);
        assertEquals(1, speech.runnables.size());
        speech.runnables.get(0).run();
        speech.runnables.get(0).run();
        assertEquals(1, outcome.spoken);
        assertEquals(0, outcome.failed);
        engine.shutdownNow();
    }

    @Test
    public void errorCallbackReportsNonEmptyFailureOnce() throws Exception {
        ExecutorService engine = new DirectExecutorService();
        RecordingExecutor speech = new RecordingExecutor();
        AndroidTextToSpeechSynthesizer synthesizer = new AndroidTextToSpeechSynthesizer(
            RuntimeEnvironment.getApplication(), engine, speech);
        RecordingOutcome outcome = new RecordingOutcome();
        synthesizer.initialize(() -> { });
        synthesizer.speak("hello", TextToSpeech.QUEUE_FLUSH, "utterance", outcome);
        UtteranceProgressListener listener = progressListener(synthesizer);
        listener.onError(registeredId(synthesizer));
        assertEquals(1, speech.runnables.size());
        assertEquals(0, outcome.failed);
        speech.runnables.get(0).run();
        speech.runnables.get(0).run();
        assertEquals(0, outcome.spoken);
        assertEquals(1, outcome.failed);
        assertFalse(outcome.reason.isEmpty());
        engine.shutdownNow();
    }

    @Test
    public void stopCallbackReportsInterruptedFailureOnce() throws Exception {
        ExecutorService engine = new DirectExecutorService();
        RecordingExecutor speech = new RecordingExecutor();
        AndroidTextToSpeechSynthesizer synthesizer = new AndroidTextToSpeechSynthesizer(
            RuntimeEnvironment.getApplication(), engine, speech);
        RecordingOutcome outcome = new RecordingOutcome();
        synthesizer.initialize(() -> { });
        synthesizer.speak("hello", TextToSpeech.QUEUE_FLUSH, "utterance", outcome);
        UtteranceProgressListener listener = progressListener(synthesizer);
        listener.onStop(registeredId(synthesizer), true);
        listener.onStop(registeredId(synthesizer), true);
        assertEquals(2, speech.runnables.size());
        speech.runnables.get(0).run();
        speech.runnables.get(1).run();
        assertEquals(0, outcome.spoken);
        assertEquals(1, outcome.failed);
        assertEquals(AndroidTextToSpeechSynthesizer.TTS_INTERRUPTED_REASON, outcome.reason);
        engine.shutdownNow();
    }

    @Test
    public void stopReportsPendingUtteranceInterruptedOnce() throws Exception {
        ExecutorService engine = new DirectExecutorService();
        AndroidTextToSpeechSynthesizer synthesizer = new AndroidTextToSpeechSynthesizer(
            RuntimeEnvironment.getApplication(), engine, Runnable::run);
        RecordingOutcome outcome = new RecordingOutcome();
        synthesizer.initialize(() -> { });
        synthesizer.speak("hello", TextToSpeech.QUEUE_FLUSH, "utterance", outcome);
        synthesizer.stop();
        assertEquals(0, outcome.spoken);
        assertEquals(1, outcome.failed);
        assertEquals(AndroidTextToSpeechSynthesizer.TTS_INTERRUPTED_REASON, outcome.reason);
        engine.shutdownNow();
    }

    @Test
    public void flushInterruptsEarlierPendingUtterancesAndKeepsNewUtterancePending() throws Exception {
        ExecutorService engine = new DirectExecutorService();
        AndroidTextToSpeechSynthesizer synthesizer = new AndroidTextToSpeechSynthesizer(
            RuntimeEnvironment.getApplication(), engine, Runnable::run);
        RecordingOutcome first = new RecordingOutcome();
        RecordingOutcome second = new RecordingOutcome();
        synthesizer.initialize(() -> { });
        synthesizer.speak("first", TextToSpeech.QUEUE_ADD, "first", first);
        synthesizer.speak("second", TextToSpeech.QUEUE_FLUSH, "second", second);
        assertEquals(1, first.failed);
        assertEquals(AndroidTextToSpeechSynthesizer.TTS_INTERRUPTED_REASON, first.reason);
        assertEquals(0, second.spoken);
        assertEquals(0, second.failed);
        assertEquals(1, pendingCount(synthesizer));
        engine.shutdownNow();
    }

    @Test
    public void lateDoneAndErrorCallbacksAfterInterruptionReportNothingFurther() throws Exception {
        ExecutorService engine = new DirectExecutorService();
        AndroidTextToSpeechSynthesizer synthesizer = new AndroidTextToSpeechSynthesizer(
            RuntimeEnvironment.getApplication(), engine, Runnable::run);
        RecordingOutcome outcome = new RecordingOutcome();
        synthesizer.initialize(() -> { });
        synthesizer.speak("hello", TextToSpeech.QUEUE_FLUSH, "utterance", outcome);
        String id = registeredId(synthesizer);
        synthesizer.stop();
        UtteranceProgressListener listener = progressListener(synthesizer);
        listener.onDone(id);
        listener.onError(id);
        assertEquals(0, outcome.spoken);
        assertEquals(1, outcome.failed);
        engine.shutdownNow();
    }

    @Test
    public void speakingBeforeEngineInitializationReportsFailure() throws Exception {
        QueuedExecutorService engine = new QueuedExecutorService();
        AndroidTextToSpeechSynthesizer synthesizer = new AndroidTextToSpeechSynthesizer(
            RuntimeEnvironment.getApplication(), engine, Runnable::run);
        RecordingOutcome outcome = new RecordingOutcome();
        synthesizer.speak("hello", TextToSpeech.QUEUE_FLUSH, "utterance", outcome);
        String registeredId = registeredId(synthesizer);
        aliasRegisteredId(synthesizer, registeredId, "utterance-" + registeredId);
        engine.runNext();
        engine.shutdown();
        assertEquals(0, outcome.spoken);
        assertEquals(1, outcome.failed);
        assertEquals(AndroidTextToSpeechSynthesizer.ENGINE_NOT_INITIALIZED_REASON, outcome.reason);
    }

    @SuppressWarnings("unchecked")
    private String registeredId(AndroidTextToSpeechSynthesizer synthesizer) throws Exception {
        Field completions = AndroidTextToSpeechSynthesizer.class.getDeclaredField("mCompletions");
        completions.setAccessible(true);
        Field map = TtsUtteranceCompletions.class.getDeclaredField("mOutcomesByUtteranceId");
        map.setAccessible(true);
        return (String) ((Map<String, ?>) map.get(completions.get(synthesizer))).keySet().iterator().next();
    }

    @SuppressWarnings("unchecked")
    private void aliasRegisteredId(AndroidTextToSpeechSynthesizer synthesizer, String oldId, String newId)
        throws Exception {
        Field completions = AndroidTextToSpeechSynthesizer.class.getDeclaredField("mCompletions");
        completions.setAccessible(true);
        Field map = TtsUtteranceCompletions.class.getDeclaredField("mOutcomesByUtteranceId");
        map.setAccessible(true);
        Map<String, TtsUtteranceOutcome> outcomes = (Map<String, TtsUtteranceOutcome>) map.get(completions.get(synthesizer));
        outcomes.put(newId, outcomes.remove(oldId));
    }

    @SuppressWarnings("unchecked")
    private int pendingCount(AndroidTextToSpeechSynthesizer synthesizer) throws Exception {
        Field completions = AndroidTextToSpeechSynthesizer.class.getDeclaredField("mCompletions");
        completions.setAccessible(true);
        Field map = TtsUtteranceCompletions.class.getDeclaredField("mOutcomesByUtteranceId");
        map.setAccessible(true);
        return ((Map<String, ?>) map.get(completions.get(synthesizer))).size();
    }

    private UtteranceProgressListener progressListener(AndroidTextToSpeechSynthesizer synthesizer)
        throws Exception {
        Field textToSpeech = AndroidTextToSpeechSynthesizer.class.getDeclaredField("mTextToSpeech");
        textToSpeech.setAccessible(true);
        Object engine = textToSpeech.get(synthesizer);
        assertNotNull(engine);
        for (Field field : TextToSpeech.class.getDeclaredFields()) {
            if (UtteranceProgressListener.class.isAssignableFrom(field.getType())) {
                field.setAccessible(true);
                return (UtteranceProgressListener) field.get(engine);
            }
        }
        throw new AssertionError("TextToSpeech utterance listener field was not found");
    }

    private static final class RecordingExecutor implements Executor {
        final java.util.List<Runnable> runnables = new java.util.ArrayList<>();
        public void execute(Runnable runnable) { runnables.add(runnable); }
    }

    private static final class DirectExecutorService extends AbstractExecutorService {
        private boolean shutdown;
        public void shutdown() { shutdown = true; }
        public List<Runnable> shutdownNow() { shutdown = true; return Collections.emptyList(); }
        public boolean isShutdown() { return shutdown; }
        public boolean isTerminated() { return shutdown; }
        public boolean awaitTermination(long timeout, java.util.concurrent.TimeUnit unit) { return shutdown; }
        public void execute(Runnable runnable) { if (!shutdown) runnable.run(); }
    }

    private static final class QueuedExecutorService extends AbstractExecutorService {
        private final List<Runnable> queued = new ArrayList<>();
        private boolean shutdown;
        public void shutdown() { shutdown = true; }
        public List<Runnable> shutdownNow() { shutdown = true; return Collections.emptyList(); }
        public boolean isShutdown() { return shutdown; }
        public boolean isTerminated() { return shutdown; }
        public boolean awaitTermination(long timeout, java.util.concurrent.TimeUnit unit) { return shutdown; }
        public void execute(Runnable runnable) { queued.add(runnable); }
        void runNext() { queued.remove(0).run(); }
    }

    private static final class RecordingOutcome implements TtsUtteranceOutcome {
        int spoken;
        int failed;
        String reason = "";
        public void onSpoken() { spoken++; }
        public void onFailed(String reason) { failed++; this.reason = reason; }
    }
}
