package com.termux.app.diagnostics;

import androidx.annotation.NonNull;

import com.termux.terminal.TranscriptWorkCostCounter;

/**
 * Process-lifetime holder for the counter that measures the open-tag scan performed on the main
 * thread for the session the owner is currently viewing, so the diagnostics report can show how
 * that cost grows with the accumulated transcript of that one session.
 */
public final class ForegroundOpenTagScanCostCounterHolder {

    private static final TranscriptWorkCostCounter INSTANCE = new TranscriptWorkCostCounter();

    private ForegroundOpenTagScanCostCounterHolder() {
    }

    @NonNull
    public static TranscriptWorkCostCounter getInstance() {
        return INSTANCE;
    }
}
