package com.termux.app.diagnostics;

import androidx.annotation.NonNull;

public final class DiagnosticEventLogHolder {

    private static final DiagnosticEventLog INSTANCE = new DiagnosticEventLog();

    private DiagnosticEventLogHolder() {
    }

    @NonNull
    public static DiagnosticEventLog getInstance() {
        return INSTANCE;
    }

    public static void record(@NonNull DiagnosticEventType type, @NonNull String detail) {
        INSTANCE.record(System.currentTimeMillis(), type, detail);
    }
}
