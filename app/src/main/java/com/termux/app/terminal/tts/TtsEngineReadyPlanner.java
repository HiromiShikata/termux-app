package com.termux.app.terminal.tts;

import androidx.annotation.NonNull;

import java.util.concurrent.Executor;

/**
 * The text-to-speech engine calls back on the thread that bound it, which is the main thread, and
 * applying a language there makes the engine hand back its voice list, whose entries carry a
 * serialized locale. Deserializing that list took most of a second on the owner's device, so the
 * language is applied on a worker and the caller is told the engine is ready afterwards, back on the
 * thread that owns the pending speech queue.
 */
final class TtsEngineReadyPlanner {

    void onEngineInitialized(boolean engineInitializedSuccessfully,
                             @NonNull Executor engineWorkExecutor,
                             @NonNull Executor speechQueueExecutor,
                             @NonNull Runnable applyDefaultLanguage,
                             @NonNull Runnable onReady) {
        if (!engineInitializedSuccessfully) {
            return;
        }
        engineWorkExecutor.execute(() -> {
            applyDefaultLanguage.run();
            speechQueueExecutor.execute(onReady);
        });
    }
}
