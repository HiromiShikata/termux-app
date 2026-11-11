package com.termux.app.terminal.tts;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import androidx.annotation.VisibleForTesting;

import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class AndroidTextToSpeechSynthesizer implements TtsManager.SpeechSynthesizer {

    static final String ENGINE_ERROR_REASON = "tts_engine_error";
    static final String ENGINE_NOT_INITIALIZED_REASON = "tts_engine_not_initialized";
    static final String TTS_INTERRUPTED_REASON = "tts_interrupted";

    private interface EngineWork {
        void runOn(TextToSpeech textToSpeech);
    }

    private final Context mContext;

    private final TtsEngineReadyPlanner mReadyPlanner = new TtsEngineReadyPlanner();

    private final TtsEngineWorkExecutor mEngineWorkExecutor;

    private final Executor mSpeechQueueExecutor;

    private final TtsUtteranceCompletions mCompletions = new TtsUtteranceCompletions();

    private volatile TextToSpeech mTextToSpeech;

    AndroidTextToSpeechSynthesizer(Context context) {
        this(context, engineWorkExecutor(), runnable -> new Handler(Looper.getMainLooper()).post(runnable));
    }

    AndroidTextToSpeechSynthesizer(Context context, ExecutorService engineWorkExecutor,
                                   Executor speechQueueExecutor) {
        mContext = context;
        mEngineWorkExecutor = new TtsEngineWorkExecutor(engineWorkExecutor);
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
        initialize(onReady, () -> {
        });
    }

    @Override
    public void initialize(Runnable onReady, Runnable onFailed) {
        mTextToSpeech = new TextToSpeech(mContext, status -> {
            if (status != TextToSpeech.SUCCESS) {
                onFailed.run();
            }
            onEngineInitialized(status, onReady);
        });
        mTextToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
            }

            @Override
            public void onDone(String utteranceId) {
                finishOnSpeechQueue(() -> mCompletions.finishSpoken(utteranceId));
            }

            @Override
            public void onStop(String utteranceId, boolean interrupted) {
                finishOnSpeechQueue(() -> mCompletions.finishFailed(utteranceId, TTS_INTERRUPTED_REASON));
            }

            @Override
            public void onError(String utteranceId) {
                Log.w(TtsManager.LOG_TAG, "TextToSpeech failed to speak utterance " + utteranceId);
                finishOnSpeechQueue(() -> mCompletions.finishFailed(utteranceId, ENGINE_ERROR_REASON));
            }
        });
    }

    private void finishOnSpeechQueue(Runnable finish) {
        mSpeechQueueExecutor.execute(finish);
    }

    @VisibleForTesting
    void onEngineInitialized(int status, Runnable onReady) {
        boolean engineInitializedSuccessfully = status == TextToSpeech.SUCCESS;
        if (!engineInitializedSuccessfully) {
            Log.w(TtsManager.LOG_TAG, "TextToSpeech engine failed to initialize with status " + status);
        }
        mReadyPlanner.onEngineInitialized(engineInitializedSuccessfully, mEngineWorkExecutor,
            mSpeechQueueExecutor, this::applyDefaultLanguage, onReady);
    }

    private void applyDefaultLanguage() {
        TextToSpeech textToSpeech = mTextToSpeech;
        if (textToSpeech == null) return;
        textToSpeech.setLanguage(Locale.getDefault());
    }

    private void onTheEngineWorkThread(EngineWork engineWork) {
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
    public void speak(String text, int queueMode, String utteranceId, TtsUtteranceOutcome outcome) {
        if (queueMode == TextToSpeech.QUEUE_FLUSH) {
            failPendingAsInterrupted();
        }
        String uniqueUtteranceId = mCompletions.register(outcome);
        mEngineWorkExecutor.execute(() -> {
            TextToSpeech textToSpeech = mTextToSpeech;
            if (textToSpeech == null) {
                mCompletions.finishFailed(uniqueUtteranceId, ENGINE_NOT_INITIALIZED_REASON);
                return;
            }
            textToSpeech.speak(text, queueMode, null, uniqueUtteranceId);
        });
    }

    @Override
    public void stop() {
        failPendingAsInterrupted();
        onTheEngineWorkThread(TextToSpeech::stop);
    }

    private void failPendingAsInterrupted() {
        for (TtsUtteranceOutcome outcome : mCompletions.takeAll()) {
            finishOnSpeechQueue(() -> outcome.onFailed(TTS_INTERRUPTED_REASON));
        }
    }

    @Override
    public void shutdown() {
        onTheEngineWorkThread(TextToSpeech::shutdown);
        mEngineWorkExecutor.stop();
    }
}
