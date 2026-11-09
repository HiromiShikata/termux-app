package com.termux.app.terminal.tts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class TtsManagerSpeakCompletionTest {

    private static final class RecordingSynthesizer implements TtsManager.SpeechSynthesizer {
        final List<String> spokenTexts = new ArrayList<>();
        final List<Runnable> completions = new ArrayList<>();
        private Runnable mOnReady;

        @Override
        public void initialize(Runnable onReady) {
            mOnReady = onReady;
        }

        void becomeReady() {
            mOnReady.run();
        }

        @Override
        public void speak(String text, int queueMode, String utteranceId) {
            spokenTexts.add(text);
            completions.add(() -> {
            });
        }

        @Override
        public void speak(String text, int queueMode, String utteranceId, Runnable onDone) {
            spokenTexts.add(text);
            completions.add(onDone);
        }

        @Override
        public void stop() {
        }

        @Override
        public void shutdown() {
        }
    }

    @Test
    public void completionCallbackRunsWhenTheSynthesizerReportsTheUtteranceEnded() {
        RecordingSynthesizer synthesizer = new RecordingSynthesizer();
        TtsManager manager = new TtsManager(synthesizer);
        synthesizer.becomeReady();
        int[] doneCount = {0};

        manager.speak("read this", () -> doneCount[0]++);

        assertEquals(Arrays.asList("read this"), synthesizer.spokenTexts);
        assertEquals(0, doneCount[0]);
        synthesizer.completions.get(0).run();
        assertEquals(1, doneCount[0]);
    }

    @Test
    public void completionCallbackIsDeferredUntilTheSynthesizerIsReady() {
        RecordingSynthesizer synthesizer = new RecordingSynthesizer();
        TtsManager manager = new TtsManager(synthesizer);
        int[] doneCount = {0};

        manager.speak("queued", () -> doneCount[0]++);
        assertTrue(synthesizer.spokenTexts.isEmpty());

        synthesizer.becomeReady();
        assertEquals(Arrays.asList("queued"), synthesizer.spokenTexts);
        synthesizer.completions.get(0).run();
        assertEquals(1, doneCount[0]);
    }

    @Test
    public void emptyTextCompletesImmediatelySoThePatrolDoesNotStall() {
        RecordingSynthesizer synthesizer = new RecordingSynthesizer();
        TtsManager manager = new TtsManager(synthesizer);
        synthesizer.becomeReady();
        int[] doneCount = {0};

        manager.speak("", () -> doneCount[0]++);

        assertTrue(synthesizer.spokenTexts.isEmpty());
        assertEquals(1, doneCount[0]);
    }

    @Test
    public void pinnedPlainSpeakStillUsesTheSynthesizerWithoutCallback() {
        RecordingSynthesizer synthesizer = new RecordingSynthesizer();
        TtsManager manager = new TtsManager(synthesizer);
        synthesizer.becomeReady();

        manager.speak("plain");

        assertEquals(Arrays.asList("plain"), synthesizer.spokenTexts);
    }
}
