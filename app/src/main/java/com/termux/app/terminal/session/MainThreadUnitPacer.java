package com.termux.app.terminal.session;

import androidx.annotation.NonNull;

import java.util.ArrayDeque;

public final class MainThreadUnitPacer {

    public static final long MAIN_THREAD_FRAME_YIELD_INTERVAL_MILLIS = 16L;

    public interface MainThreadMessagePoster {
        void postToMainThreadDelayed(@NonNull Runnable runnable, long delayMillis);
    }

    private final MainThreadMessagePoster mainThreadMessagePoster;

    private final ArrayDeque<Runnable> pendingUnits = new ArrayDeque<>();

    private boolean unitMessagePosted;

    public MainThreadUnitPacer(@NonNull MainThreadMessagePoster mainThreadMessagePoster) {
        this.mainThreadMessagePoster = mainThreadMessagePoster;
    }

    public void enqueueUnit(@NonNull Runnable unit) {
        pendingUnits.add(unit);
        postNextUnitMessageIfIdle();
    }

    private void postNextUnitMessageIfIdle() {
        if (unitMessagePosted) return;
        if (pendingUnits.isEmpty()) return;
        unitMessagePosted = true;
        mainThreadMessagePoster.postToMainThreadDelayed(
            this::runNextUnit, MAIN_THREAD_FRAME_YIELD_INTERVAL_MILLIS);
    }

    private void runNextUnit() {
        unitMessagePosted = false;
        try {
            Runnable unit = pendingUnits.poll();
            if (unit != null) unit.run();
        } finally {
            postNextUnitMessageIfIdle();
        }
    }
}
