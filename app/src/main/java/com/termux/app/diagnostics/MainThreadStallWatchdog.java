package com.termux.app.diagnostics;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import androidx.annotation.NonNull;

public final class MainThreadStallWatchdog {

    private static final long STALL_THRESHOLD_MILLIS = 80L;
    private static final long HEARTBEAT_INTERVAL_MILLIS = 200L;
    private static final long SAMPLE_POLL_INTERVAL_MILLIS = 20L;

    private static final MainThreadStallSampleSchedule SAMPLE_SCHEDULE =
        new MainThreadStallSampleSchedule(STALL_THRESHOLD_MILLIS, SAMPLE_POLL_INTERVAL_MILLIS);

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
            long heartbeatPostedAtMillis = SystemClock.uptimeMillis();
            while (!isAnswered(heartbeatAnswered)) {
                long sampledAtMillis = SystemClock.uptimeMillis();
                if (recorder.needsStackSample(sampledAtMillis)) {
                    recorder.sampleWhileOutstanding(sampledAtMillis, mainThread.getStackTrace());
                }
                if (!sleep(SAMPLE_SCHEDULE.sleepMillisAfterAttemptAt(
                        sampledAtMillis - heartbeatPostedAtMillis))) {
                    return;
                }
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
