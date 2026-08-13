package com.termux.view.scroll;

public final class TerminalScrollStepCounterHolder {

    private static final TerminalScrollStepCounter INSTANCE = new TerminalScrollStepCounter();

    private TerminalScrollStepCounterHolder() {
    }

    public static TerminalScrollStepCounter getInstance() {
        return INSTANCE;
    }
}
