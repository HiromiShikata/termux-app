package com.termux.app.terminal.tts;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.Locale;

final class AndroidTextToSpeechSynthesizer implements TtsManager.SpeechSynthesizer {

    private final Context mContext;

    private TextToSpeech mTextToSpeech;

    AndroidTextToSpeechSynthesizer(Context context) {
        mContext = context;
    }

    @Override
    public void initialize(Runnable onReady) {
        mTextToSpeech = new TextToSpeech(mContext, status -> {
            if (status != TextToSpeech.SUCCESS) {
                Log.w(TtsManager.LOG_TAG, "TextToSpeech engine failed to initialize with status " + status);
                return;
            }
            mTextToSpeech.setLanguage(Locale.getDefault());
            onReady.run();
        });
    }

    @Override
    public void speak(String text, int queueMode, String utteranceId) {
        if (mTextToSpeech == null) return;
        mTextToSpeech.speak(text, queueMode, null, utteranceId);
    }

    @Override
    public void stop() {
        if (mTextToSpeech != null) mTextToSpeech.stop();
    }

    @Override
    public void shutdown() {
        if (mTextToSpeech != null) mTextToSpeech.shutdown();
    }
}
