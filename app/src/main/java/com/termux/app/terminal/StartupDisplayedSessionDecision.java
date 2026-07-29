package com.termux.app.terminal;

public final class StartupDisplayedSessionDecision {

    private StartupDisplayedSessionDecision() {
    }

    public static int sessionIndexToDisplay(int topmostNonHiddenSessionIndex, int fallbackSessionIndex) {
        return topmostNonHiddenSessionIndex >= 0 ? topmostNonHiddenSessionIndex : fallbackSessionIndex;
    }
}
