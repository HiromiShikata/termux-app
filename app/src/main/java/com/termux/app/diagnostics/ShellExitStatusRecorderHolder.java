package com.termux.app.diagnostics;

import androidx.annotation.NonNull;

public final class ShellExitStatusRecorderHolder {

    private static final ShellExitStatusRecorder INSTANCE = new ShellExitStatusRecorder();

    private ShellExitStatusRecorderHolder() {
    }

    @NonNull
    public static ShellExitStatusRecorder getInstance() {
        return INSTANCE;
    }
}
