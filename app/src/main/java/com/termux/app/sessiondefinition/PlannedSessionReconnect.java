package com.termux.app.sessiondefinition;

import androidx.annotation.NonNull;

public final class PlannedSessionReconnect {

    private final String sessionName;

    private final SessionReconnectReason reason;

    public PlannedSessionReconnect(@NonNull String sessionName, @NonNull SessionReconnectReason reason) {
        this.sessionName = sessionName;
        this.reason = reason;
    }

    @NonNull
    public String getSessionName() {
        return sessionName;
    }

    @NonNull
    public SessionReconnectReason getReason() {
        return reason;
    }
}
