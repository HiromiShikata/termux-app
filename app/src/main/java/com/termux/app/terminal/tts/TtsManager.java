package com.termux.app.terminal.tts;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public final class TtsManager {

    public static final String LOG_TAG = "TermuxSpeakTagTts";

    static final String UTTERANCE_ID = "termux-speak-tag";

    public interface SpeechSynthesizer {
        void initialize(Runnable onReady);

        void speak(String text, int queueMode, String utteranceId);

        default void speak(String text, int queueMode, String utteranceId, Runnable onDone) {
            speak(text, queueMode, utteranceId);
        }

        void stop();

        void shutdown();
    }

    private final SpeechSynthesizer mSynthesizer;
    private final List<Utterance> mPendingUtterances = new ArrayList<>();

    private boolean mReady = false;

    public TtsManager(Context context) {
        this(new AndroidTextToSpeechSynthesizer(context.getApplicationContext()));
    }

    TtsManager(SpeechSynthesizer synthesizer) {
        mSynthesizer = synthesizer;
        mSynthesizer.initialize(this::onReady);
    }

    private void onReady() {
        mReady = true;
        List<Utterance> pending = new ArrayList<>(mPendingUtterances);
        mPendingUtterances.clear();
        for (Utterance utterance : pending) dispatch(utterance);
    }

    public void speak(String text) {
        if (text == null || text.isEmpty()) return;
        speak(text, () -> {
        });
    }

    public void speak(String text, Runnable onDone) {
        if (text == null || text.isEmpty()) {
            onDone.run();
            return;
        }
        Log.i(LOG_TAG, "speak (QUEUE_FLUSH): " + text);
        Utterance utterance = new Utterance(text, onDone);
        if (mReady) {
            dispatch(utterance);
        } else {
            mPendingUtterances.add(utterance);
        }
    }

    public void stop() {
        Log.i(LOG_TAG, "stop");
        mPendingUtterances.clear();
        mSynthesizer.stop();
    }

    public void shutdown() {
        mSynthesizer.stop();
        mSynthesizer.shutdown();
    }

    private void dispatch(Utterance utterance) {
        mSynthesizer.speak(utterance.text, TextToSpeech.QUEUE_FLUSH, UTTERANCE_ID, utterance.onDone);
    }

    private static final class Utterance {
        final String text;
        final Runnable onDone;

        Utterance(String text, Runnable onDone) {
            this.text = text;
            this.onDone = onDone;
        }
    }
}
