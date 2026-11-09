package com.termux.app.voicepatrol;

import android.os.Handler;
import android.os.Looper;

public final class VoicePatrolRuntime {

    static final long POLL_INTERVAL_MILLIS = 2000L;

    private static VoicePatrolController sActiveController;

    private static final Handler sMainHandler = new Handler(Looper.getMainLooper());

    private static final Runnable sPollCallingSessions = new Runnable() {
        @Override
        public void run() {
            VoicePatrolController controller = sActiveController;
            if (controller == null) {
                return;
            }
            controller.onCallingSessionsChanged();
            sMainHandler.postDelayed(this, POLL_INTERVAL_MILLIS);
        }
    };

    private VoicePatrolRuntime() {
    }

    public static boolean isRunning() {
        return sActiveController != null;
    }

    public static void start(VoicePatrolController controller) {
        stop();
        sActiveController = controller;
        controller.start();
        sMainHandler.postDelayed(sPollCallingSessions, POLL_INTERVAL_MILLIS);
    }

    public static void stop() {
        sMainHandler.removeCallbacks(sPollCallingSessions);
        VoicePatrolController controller = sActiveController;
        sActiveController = null;
        if (controller != null) {
            controller.stop();
        }
    }
}
