package com.termux.app.diagnostics;

import androidx.annotation.NonNull;

public final class ActivityWindowRecorderHolder {

    private static final ActivityWindowRecorder INSTANCE = new ActivityWindowRecorder();

    private ActivityWindowRecorderHolder() {
    }

    @NonNull
    public static ActivityWindowRecorder getInstance() {
        return INSTANCE;
    }
}
