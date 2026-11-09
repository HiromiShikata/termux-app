package com.termux.app.terminal.tts;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class TtsManager {

    public static final String LOG_TAG = "TermuxSpeakTagTts";

    static final String UTTERANCE_ID = "termux-speak-tag";

    static final String ENGINE_UNAVAILABLE_REASON = "tts_engine_unavailable";

    public interface SpeechSynthesizer {
        void initialize(Runnable onReady);

        default void initialize(Runnable onReady, Runnable onFailed) {
            initialize(onReady);
        }

        void speak(String text, int queueMode, String utteranceId);

        default void speak(String text, int queueMode, String utteranceId, TtsUtteranceOutcome outcome) {
            speak(text, queueMode, utteranceId);
        }

        void stop();

        void shutdown();
    }

    private final SpeechSynthesizer mSynthesizer;
    private final List<Utterance> mPendingUtterances = new ArrayList<>();

    private boolean mReady = false;
    private boolean mFailed = false;

    public TtsManager(Context context) {
        this(new AndroidTextToSpeechSynthesizer(context.getApplicationContext()));
    }

    protected TtsManager(SpeechSynthesizer synthesizer) {
        mSynthesizer = synthesizer;
        mSynthesizer.initialize(this::onReady, this::onInitializationFailed);
    }

    private void onReady() {
        mReady = true;
        List<Utterance> pending = new ArrayList<>(mPendingUtterances);
        mPendingUtterances.clear();
        for (Utterance utterance : pending) dispatch(utterance);
    }

    private void onInitializationFailed() {
        mFailed = true;
        Log.w(LOG_TAG, "TextToSpeech engine failed to initialize");
        List<Utterance> pending = new ArrayList<>(mPendingUtterances);
        mPendingUtterances.clear();
        for (Utterance utterance : pending) utterance.outcome.onFailed(ENGINE_UNAVAILABLE_REASON);
    }

    public void speak(String text) {
        if (text == null || text.isEmpty()) return;
        speak(text, new TtsUtteranceOutcome() {
            @Override
            public void onSpoken() {
            }

            @Override
            public void onFailed(String reason) {
                Log.w(LOG_TAG, "speech failed: " + reason);
            }
        });
    }

    public void speak(String text, TtsUtteranceOutcome outcome) {
        if (text == null || text.isEmpty()) {
            outcome.onSpoken();
            return;
        }
        if (mFailed) {
            outcome.onFailed(ENGINE_UNAVAILABLE_REASON);
            return;
        }
        Log.i(LOG_TAG, "speak (QUEUE_FLUSH): " + text);
        Utterance utterance = new Utterance(text, outcome);
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
        mSynthesizer.speak(utterance.text, TextToSpeech.QUEUE_FLUSH, UTTERANCE_ID, utterance.outcome);
    }

    private static final class Utterance {
        final String text;
        final TtsUtteranceOutcome outcome;

        Utterance(String text, TtsUtteranceOutcome outcome) {
            this.text = text;
            this.outcome = outcome;
        }
    }
}
