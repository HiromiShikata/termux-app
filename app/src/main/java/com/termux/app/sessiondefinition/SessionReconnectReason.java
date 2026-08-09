package com.termux.app.sessiondefinition;

import androidx.annotation.NonNull;

public enum SessionReconnectReason {

    SHELL_PROCESS_EXITED("Shell process exited"),

    SHELL_PROCESS_GONE_AT_THE_BACKGROUND_SCAN("Shell process gone at the background scan"),

    INPUT_NO_LONGER_REACHES_THE_PROGRAM("Input no longer reaches the program"),

    SILENT_FOR_LONGER_THAN_THE_STALENESS_THRESHOLD("Silent for longer than the staleness threshold");

    private final String reportLabel;

    SessionReconnectReason(@NonNull String reportLabel) {
        this.reportLabel = reportLabel;
    }

    @NonNull
    public String getReportLabel() {
        return reportLabel;
    }
}
