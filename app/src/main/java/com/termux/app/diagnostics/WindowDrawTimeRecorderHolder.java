package com.termux.app.diagnostics;

import androidx.annotation.NonNull;

public final class WindowDrawTimeRecorderHolder {

    private static final DrawTimeRecorder INSTANCE = new DrawTimeRecorder();

    private WindowDrawTimeRecorderHolder() {
    }

    @NonNull
    public static DrawTimeRecorder getInstance() {
        return INSTANCE;
    }
}
