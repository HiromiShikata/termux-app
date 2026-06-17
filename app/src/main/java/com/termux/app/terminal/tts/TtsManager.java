package com.termux.app.terminal.tts;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class TtsManager {

    public static final String LOG_TAG = "TermuxSpeakTagTts";

    private static final String UTTERANCE_ID = "termux-speak-tag";

    private final TextToSpeech mTextToSpeech;
    private final List<String> mPendingTexts = new ArrayList<>();

    private boolean mReady = false;

    public TtsManager(Context context) {
        mTextToSpeech = new TextToSpeech(context.getApplicationContext(), this::onInit);
    }

    private void onInit(int status) {
        if (status != TextToSpeech.SUCCESS) {
            Log.w(LOG_TAG, "TextToSpeech engine failed to initialize with status " + status);
            return;
        }
        mTextToSpeech.setLanguage(Locale.getDefault());
        mReady = true;
        List<String> pending = new ArrayList<>(mPendingTexts);
        mPendingTexts.clear();
        for (String text : pending) dispatch(text);
    }

    public void speak(String text) {
        if (text == null || text.isEmpty()) return;
        Log.i(LOG_TAG, "speak: " + text);
        if (mReady) {
            dispatch(text);
        } else {
            mPendingTexts.add(text);
        }
    }

    public void shutdown() {
        mTextToSpeech.stop();
        mTextToSpeech.shutdown();
    }

    private void dispatch(String text) {
        mTextToSpeech.speak(text, TextToSpeech.QUEUE_ADD, null, UTTERANCE_ID);
    }
}
