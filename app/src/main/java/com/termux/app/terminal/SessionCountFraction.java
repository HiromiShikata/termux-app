package com.termux.app.terminal;

import androidx.annotation.NonNull;

public final class SessionCountFraction {

    private SessionCountFraction() {
    }

    @NonNull
    public static String of(int pendingCallSessionCount, int totalSessionCount) {
        return "(" + pendingCallSessionCount + "/" + totalSessionCount + ")";
    }
}
