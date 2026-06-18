package com.termux.app.terminal.tts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.speech.tts.TextToSpeech;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class TtsManagerTest {

    private static final class FakeSpeechSynthesizer implements TtsManager.SpeechSynthesizer {
        final List<Integer> queueModes = new ArrayList<>();
        final List<String> spokenTexts = new ArrayList<>();
        int stopCount = 0;

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
            queueModes.add(queueMode);
        }

        @Override
        public void stop() {
            stopCount++;
        }

        @Override
        public void shutdown() {
        }
    }

    @Test
    public void speakUsesQueueFlushSoOnlyTheLatestContentIsSpoken() {
        FakeSpeechSynthesizer synthesizer = new FakeSpeechSynthesizer();
        TtsManager manager = new TtsManager(synthesizer);
        synthesizer.becomeReady();

        manager.speak("hello world");

        assertEquals(1, synthesizer.queueModes.size());
        assertEquals(TextToSpeech.QUEUE_FLUSH, (int) synthesizer.queueModes.get(0));
        assertFalse(synthesizer.queueModes.get(0) == TextToSpeech.QUEUE_ADD);
    }

    @Test
    public void textRequestedBeforeReadyIsDispatchedWithQueueFlushOnceReady() {
        FakeSpeechSynthesizer synthesizer = new FakeSpeechSynthesizer();
        TtsManager manager = new TtsManager(synthesizer);

        manager.speak("before ready");
        assertTrue(synthesizer.spokenTexts.isEmpty());

        synthesizer.becomeReady();
        assertEquals(1, synthesizer.spokenTexts.size());
        assertEquals(TextToSpeech.QUEUE_FLUSH, (int) synthesizer.queueModes.get(0));
    }

    @Test
    public void stopClearsPendingTextAndStopsTheEngine() {
        FakeSpeechSynthesizer synthesizer = new FakeSpeechSynthesizer();
        TtsManager manager = new TtsManager(synthesizer);

        manager.speak("queued before ready");
        manager.stop();
        synthesizer.becomeReady();

        assertTrue(synthesizer.spokenTexts.isEmpty());
        assertEquals(1, synthesizer.stopCount);
    }
}
