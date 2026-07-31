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
        startWatching(RECORDER);
    }

    @NonNull
    static Thread startWatching(@NonNull MainThreadStallRecorder recorder) {
        Thread watchdogThread = new Thread(() -> watch(recorder), "main-thread-stall-watchdog");
        watchdogThread.setDaemon(true);
        watchdogThread.setPriority(Thread.MIN_PRIORITY);
        watchdogThread.start();
        return watchdogThread;
    }

    private static void watch(@NonNull MainThreadStallRecorder recorder) {
        Looper mainLooper = Looper.getMainLooper();
        Handler mainHandler = new Handler(mainLooper);
        Thread mainThread = mainLooper.getThread();
        while (true) {
            final boolean[] heartbeatAnswered = new boolean[1];
            recorder.heartbeatPosted(SystemClock.uptimeMillis());
            mainHandler.post(() -> {
                synchronized (heartbeatAnswered) {
                    heartbeatAnswered[0] = true;
                }
                recorder.heartbeatRan(SystemClock.uptimeMillis());
            });
            while (!isAnswered(heartbeatAnswered)) {
                if (!sleep(SAMPLE_INTERVAL_MILLIS)) {
                    return;
                }
                recorder.sampleWhileOutstanding(SystemClock.uptimeMillis(), mainThread.getStackTrace());
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
