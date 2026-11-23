package com.termux.app.voiceconsole;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Holds the one running voice console session. Called on the main thread only. */
public final class VoiceConsoleRuntime {

    public interface Listener {
        void onVoiceConsoleRunningChanged(boolean running);
    }

    private static VoiceConsoleController sActiveController;
    private static final List<Listener> sListeners = new CopyOnWriteArrayList<>();

    private VoiceConsoleRuntime() {
    }

    public static boolean isRunning() {
        return sActiveController != null;
    }

    public static void addListener(Listener listener) {
        sListeners.add(listener);
    }

    public static void removeListener(Listener listener) {
        sListeners.remove(listener);
    }

    public static void start(VoiceConsoleController controller) {
        stop();
        sActiveController = controller;
        controller.start();
        if (sActiveController == controller) {
            notifyListeners(true);
        }
    }

    public static void stop() {
        VoiceConsoleController controller = sActiveController;
        if (controller == null) {
            return;
        }
        sActiveController = null;
        controller.stop();
        notifyListeners(false);
    }

    private static void notifyListeners(boolean running) {
        for (Listener listener : sListeners) {
            listener.onVoiceConsoleRunningChanged(running);
        }
    }
}
