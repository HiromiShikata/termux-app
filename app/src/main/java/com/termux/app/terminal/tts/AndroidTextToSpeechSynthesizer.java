package com.termux.app.terminal.tts;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

final class AndroidTextToSpeechSynthesizer implements TtsManager.SpeechSynthesizer {

    private final Context mContext;

    private final TtsEngineReadyPlanner mReadyPlanner = new TtsEngineReadyPlanner();

    private final Executor mEngineWorkExecutor =
        Executors.newSingleThreadExecutor(runnable -> {
            Thread worker = new Thread(runnable, "termux-tts-engine-work");
            worker.setDaemon(true);
            return worker;
        });

    private final Executor mSpeechQueueExecutor =
        runnable -> new Handler(Looper.getMainLooper()).post(runnable);

    private TextToSpeech mTextToSpeech;

    AndroidTextToSpeechSynthesizer(Context context) {
        mContext = context;
    }

    @Override
    public void initialize(Runnable onReady) {
        mTextToSpeech = new TextToSpeech(mContext, status -> {
            boolean engineInitializedSuccessfully = status == TextToSpeech.SUCCESS;
            if (!engineInitializedSuccessfully) {
                Log.w(TtsManager.LOG_TAG, "TextToSpeech engine failed to initialize with status " + status);
            }
            mReadyPlanner.onEngineInitialized(engineInitializedSuccessfully, mEngineWorkExecutor,
                mSpeechQueueExecutor, this::applyDefaultLanguage, onReady);
        });
    }

    private void applyDefaultLanguage() {
        TextToSpeech textToSpeech = mTextToSpeech;
        if (textToSpeech == null) return;
        textToSpeech.setLanguage(Locale.getDefault());
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
