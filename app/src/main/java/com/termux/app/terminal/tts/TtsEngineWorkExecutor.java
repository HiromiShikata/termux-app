package com.termux.app.terminal.tts;

import androidx.annotation.NonNull;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

/**
 * Every call into the platform speech engine is made on one worker so a stop cannot overtake the
 * speech it was meant to cancel, and the worker is stopped with the synthesizer so a synthesizer the
 * activity replaces leaves no thread behind. Binding to the speech service is asynchronous, so work
 * can still be offered after that stop, and a shut-down executor answers such an offer by throwing
 * where the offer was made. Deciding whether the worker is still running and handing it the work
 * happen together here, so no caller has to hold that decision and no caller is thrown at.
 */
final class TtsEngineWorkExecutor implements Executor {

    private final ExecutorService mWorker;

    private boolean mStopped;

    TtsEngineWorkExecutor(@NonNull ExecutorService worker) {
        mWorker = worker;
    }

    @Override
    public synchronized void execute(@NonNull Runnable engineWork) {
        if (mStopped) return;
        mWorker.execute(engineWork);
    }

    synchronized void stop() {
        mStopped = true;
        mWorker.shutdown();
    }
}
