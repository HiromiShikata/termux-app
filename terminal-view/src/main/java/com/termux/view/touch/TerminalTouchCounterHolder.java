package com.termux.view.touch;

public final class TerminalTouchCounterHolder {

    private static final TerminalTouchCounter INSTANCE = new TerminalTouchCounter();

    private TerminalTouchCounterHolder() {
    }

    public static TerminalTouchCounter getInstance() {
        return INSTANCE;
    }
}
