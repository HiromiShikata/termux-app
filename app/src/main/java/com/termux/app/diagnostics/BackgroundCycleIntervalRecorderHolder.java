package com.termux.app.diagnostics;

import androidx.annotation.NonNull;

public final class BackgroundCycleIntervalRecorderHolder {

    private static final BackgroundCycleIntervalRecorder INSTANCE =
        new BackgroundCycleIntervalRecorder();

    private BackgroundCycleIntervalRecorderHolder() {
    }

    @NonNull
    public static BackgroundCycleIntervalRecorder getInstance() {
        return INSTANCE;
    }
}
