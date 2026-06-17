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

        void stop();

        void shutdown();
    }

    private final SpeechSynthesizer mSynthesizer;
    private final List<String> mPendingTexts = new ArrayList<>();

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
        List<String> pending = new ArrayList<>(mPendingTexts);
        mPendingTexts.clear();
        for (String text : pending) dispatch(text);
    }

    public void speak(String text) {
        if (text == null || text.isEmpty()) return;
        Log.i(LOG_TAG, "speak (QUEUE_FLUSH): " + text);
        if (mReady) {
            dispatch(text);
        } else {
            mPendingTexts.add(text);
        }
    }

    public void stop() {
        Log.i(LOG_TAG, "stop");
        mPendingTexts.clear();
        mSynthesizer.stop();
    }

    public void shutdown() {
        mSynthesizer.stop();
        mSynthesizer.shutdown();
    }

    private void dispatch(String text) {
        mSynthesizer.speak(text, TextToSpeech.QUEUE_FLUSH, UTTERANCE_ID);
    }
}
