package com.termux.app.terminal;

/**
 * Decides which session index a startup path displays. The topmost non-hidden session in the
 * ordering the user sees in the session list wins, and the caller's existing choice is kept as the
 * fallback for when every session is hidden or no ordered row list is available yet.
 */
public final class StartupDisplayedSessionDecision {

    private StartupDisplayedSessionDecision() {
    }

    public static int sessionIndexToDisplay(int topmostNonHiddenSessionIndex, int fallbackSessionIndex) {
        return topmostNonHiddenSessionIndex >= 0 ? topmostNonHiddenSessionIndex : fallbackSessionIndex;
    }
}
