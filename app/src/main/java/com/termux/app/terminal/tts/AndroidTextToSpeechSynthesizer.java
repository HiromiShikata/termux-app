package com.termux.app.terminal.tts;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class AndroidTextToSpeechSynthesizer implements TtsManager.SpeechSynthesizer {

    private interface EngineWork {
        void runOn(TextToSpeech textToSpeech);
    }

    private final Context mContext;

    private final TtsEngineReadyPlanner mReadyPlanner = new TtsEngineReadyPlanner();

    private final ExecutorService mEngineWorkExecutor;

    private final Executor mSpeechQueueExecutor;

    private volatile TextToSpeech mTextToSpeech;

    private volatile boolean mEngineWorkerStopped;

    AndroidTextToSpeechSynthesizer(Context context) {
        this(context, engineWorkExecutor(), runnable -> new Handler(Looper.getMainLooper()).post(runnable));
    }

    AndroidTextToSpeechSynthesizer(Context context, ExecutorService engineWorkExecutor,
                                   Executor speechQueueExecutor) {
        mContext = context;
        mEngineWorkExecutor = engineWorkExecutor;
        mSpeechQueueExecutor = speechQueueExecutor;
    }

    private static ExecutorService engineWorkExecutor() {
        return Executors.newSingleThreadExecutor(runnable -> {
            Thread worker = new Thread(runnable, "termux-tts-engine-work");
            worker.setDaemon(true);
            return worker;
        });
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

    private void onTheEngineWorkThread(EngineWork engineWork) {
        if (mEngineWorkerStopped) return;
        mEngineWorkExecutor.execute(() -> {
            TextToSpeech textToSpeech = mTextToSpeech;
            if (textToSpeech == null) return;
            engineWork.runOn(textToSpeech);
        });
    }

    @Override
    public void speak(String text, int queueMode, String utteranceId) {
        onTheEngineWorkThread(textToSpeech -> textToSpeech.speak(text, queueMode, null, utteranceId));
    }

    @Override
    public void stop() {
        onTheEngineWorkThread(TextToSpeech::stop);
    }

    @Override
    public void shutdown() {
        onTheEngineWorkThread(TextToSpeech::shutdown);
        mEngineWorkerStopped = true;
        mEngineWorkExecutor.shutdown();
    }
}
