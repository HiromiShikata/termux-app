package com.termux.terminal;

/**
 * Process-lifetime holder for the counter that measures the buffer reflow a column-changing resize
 * performs. It lives in the terminal-emulator module because {@link TerminalBuffer} performs the
 * reflow and must not depend on the app module; the app module reads the counter for its
 * diagnostics report.
 */
public final class TerminalBufferReflowCostCounterHolder {

    private static final TranscriptWorkCostCounter INSTANCE = new TranscriptWorkCostCounter();

    private TerminalBufferReflowCostCounterHolder() {
    }

    public static TranscriptWorkCostCounter getInstance() {
        return INSTANCE;
    }
}
