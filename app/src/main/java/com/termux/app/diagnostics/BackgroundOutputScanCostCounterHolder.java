package com.termux.app.diagnostics;

import androidx.annotation.NonNull;

import com.termux.terminal.TranscriptWorkCostCounter;

/**
 * Process-lifetime holder for the counter that measures the background output tag scan performed on
 * the main thread, so the diagnostics report can show how that cost grows with accumulated
 * transcript rows.
 */
public final class BackgroundOutputScanCostCounterHolder {

    private static final TranscriptWorkCostCounter INSTANCE = new TranscriptWorkCostCounter();

    private BackgroundOutputScanCostCounterHolder() {
    }

    @NonNull
    public static TranscriptWorkCostCounter getInstance() {
        return INSTANCE;
    }
}
