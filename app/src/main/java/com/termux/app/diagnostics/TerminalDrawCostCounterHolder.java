package com.termux.app.diagnostics;

import androidx.annotation.NonNull;

import com.termux.terminal.TranscriptWorkCostCounter;

public final class TerminalDrawCostCounterHolder {

    private static final TranscriptWorkCostCounter INSTANCE = new TranscriptWorkCostCounter();

    private TerminalDrawCostCounterHolder() {
    }

    @NonNull
    public static TranscriptWorkCostCounter getInstance() {
        return INSTANCE;
    }
}
