package com.termux.shared.termux.terminal;

import androidx.annotation.NonNull;

import com.termux.terminal.TranscriptWorkCostCounter;

public final class ShellOutputParseCostCounterHolder {

    private static final TranscriptWorkCostCounter INSTANCE = new TranscriptWorkCostCounter();

    private ShellOutputParseCostCounterHolder() {
    }

    @NonNull
    public static TranscriptWorkCostCounter getInstance() {
        return INSTANCE;
    }
}
