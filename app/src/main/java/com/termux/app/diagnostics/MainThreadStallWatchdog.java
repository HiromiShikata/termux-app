package com.termux.app.diagnostics;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import androidx.annotation.NonNull;

public final class MainThreadStallWatchdog {

    private static final long STALL_THRESHOLD_MILLIS = 250L;
    private static final long HEARTBEAT_INTERVAL_MILLIS = 500L;
    private static final long SAMPLE_INTERVAL_MILLIS = 100L;

    private static final MainThreadStallRecorder RECORDER =
        new MainThreadStallRecorder(STALL_THRESHOLD_MILLIS);

    private static boolean sStarted;

    private MainThreadStallWatchdog() {
    }

    @NonNull
    public static MainThreadStallRecorder getRecorder() {
        return RECORDER;
    }

    public static synchronized void start() {
        if (sStarted) {
            return;
        }
        sStarted = true;
        Thread watchdogThread = new Thread(MainThreadStallWatchdog::watch, "main-thread-stall-watchdog");
        watchdogThread.setDaemon(true);
        watchdogThread.setPriority(Thread.MIN_PRIORITY);
        watchdogThread.start();
    }

    private static void watch() {
        Looper mainLooper = Looper.getMainLooper();
        Handler mainHandler = new Handler(mainLooper);
        Thread mainThread = mainLooper.getThread();
        while (true) {
            final boolean[] heartbeatAnswered = new boolean[1];
            RECORDER.heartbeatPosted(SystemClock.uptimeMillis());
            mainHandler.post(() -> {
                synchronized (heartbeatAnswered) {
                    heartbeatAnswered[0] = true;
                }
                RECORDER.heartbeatRan(SystemClock.uptimeMillis());
            });
            while (!isAnswered(heartbeatAnswered)) {
                if (!sleep(SAMPLE_INTERVAL_MILLIS)) {
                    return;
                }
                RECORDER.sampleWhileOutstanding(SystemClock.uptimeMillis(), mainThread.getStackTrace());
            }
            if (!sleep(HEARTBEAT_INTERVAL_MILLIS)) {
                return;
            }
        }
    }

    private static boolean isAnswered(@NonNull boolean[] heartbeatAnswered) {
        synchronized (heartbeatAnswered) {
            return heartbeatAnswered[0];
        }
    }

    private static boolean sleep(long millis) {
        try {
            Thread.sleep(millis);
            return true;
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
