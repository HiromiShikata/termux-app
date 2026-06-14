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

    public static Boolean parseMouseState(String output) {
        if (output == null) return null;
        String trimmed = output.trim();
        if ("on".equals(trimmed)) return Boolean.TRUE;
        if ("off".equals(trimmed)) return Boolean.FALSE;
        return null;
    }
}
