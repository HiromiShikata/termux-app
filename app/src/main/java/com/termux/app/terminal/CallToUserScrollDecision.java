package com.termux.app.terminal;

public final class CallToUserScrollDecision {

    private CallToUserScrollDecision() {
    }

    public static boolean allowsInAppScroll(boolean alternateBufferActive) {
        return !alternateBufferActive;
    }
}
