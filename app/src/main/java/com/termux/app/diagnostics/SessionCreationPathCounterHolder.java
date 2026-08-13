package com.termux.app.diagnostics;

import androidx.annotation.NonNull;

public final class SessionCreationPathCounterHolder {

    private static final SessionCreationPathCounter INSTANCE = new SessionCreationPathCounter();

    private SessionCreationPathCounterHolder() {
    }

    @NonNull
    public static SessionCreationPathCounter getInstance() {
        return INSTANCE;
    }
}
