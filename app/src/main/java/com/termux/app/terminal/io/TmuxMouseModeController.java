package com.termux.app.terminal.io;

public final class TmuxMouseModeController {

    public static final String[] SHOW_MOUSE_ARGUMENTS = {"show", "-gv", "mouse"};

    private TmuxMouseModeController() {
    }

    public static boolean nextState(boolean currentlyEnabled) {
        return !currentlyEnabled;
    }

    public static String[] setMouseArguments(boolean targetEnabled) {
        return new String[]{"set", "-g", "mouse", targetEnabled ? "on" : "off"};
    }

    public static boolean parseShowMouseOutput(String output, boolean fallbackEnabled) {
        if (output == null) return fallbackEnabled;
        String trimmed = output.trim();
        if ("on".equals(trimmed)) return true;
        if ("off".equals(trimmed)) return false;
        return fallbackEnabled;
    }
}
