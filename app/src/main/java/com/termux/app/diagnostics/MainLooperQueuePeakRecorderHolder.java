package com.termux.app.diagnostics;

import androidx.annotation.NonNull;

public final class MainLooperQueuePeakRecorderHolder {

    private static final MainLooperQueuePeakRecorder INSTANCE = new MainLooperQueuePeakRecorder();

    private MainLooperQueuePeakRecorderHolder() {
    }

    @NonNull
    public static MainLooperQueuePeakRecorder getInstance() {
        return INSTANCE;
    }
}
