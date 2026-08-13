package com.termux.app.diagnostics;

import androidx.annotation.NonNull;

public enum SessionCreationPath {

    RECONNECT_OF_A_DEAD_SESSION("Reconnect of a dead session"),

    RECONNECT_OF_A_FINISHED_SESSION_IN_PLACE("Reconnect of a finished session in place"),

    RESTORE_OF_A_PERSISTED_SESSION("Restore of a persisted session"),

    RESTORE_OF_AN_ALWAYS_PRESENT_SESSION("Restore of an always present session"),

    NEW_SESSION_THE_OWNER_ASKED_FOR("New session the owner asked for"),

    NEW_AUTOSSH_SESSION("New autossh session"),

    RESET_OF_A_HOST_SESSION("Reset of a host session"),

    KILL_OF_A_HOST_SESSION("Kill of a host session"),

    SESSION_STARTED_BY_AN_INTENT("Session started by an intent");

    @NonNull
    private final String mReportLabel;

    SessionCreationPath(@NonNull String reportLabel) {
        mReportLabel = reportLabel;
    }

    @NonNull
    public String getReportLabel() {
        return mReportLabel;
    }
}
