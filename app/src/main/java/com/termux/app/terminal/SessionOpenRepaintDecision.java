package com.termux.app.terminal;

public final class SessionOpenRepaintDecision {

    private SessionOpenRepaintDecision() {
    }

    public static boolean shouldForceRemoteRepaint(boolean sessionRunning, boolean hasEmulator, int columns, int rows) {
        return sessionRunning && hasEmulator && columns > 0 && rows > 0;
    }
}
