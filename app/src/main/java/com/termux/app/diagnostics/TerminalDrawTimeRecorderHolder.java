package com.termux.app.diagnostics;

import androidx.annotation.NonNull;

public final class TerminalDrawTimeRecorderHolder {

    private static final TerminalDrawTimeRecorder INSTANCE = new TerminalDrawTimeRecorder();

    private TerminalDrawTimeRecorderHolder() {
    }

    @NonNull
    public static TerminalDrawTimeRecorder getInstance() {
        return INSTANCE;
    }
}
