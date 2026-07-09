package com.termux.terminal;

public final class WrappedLineLocation {

    public final String text;
    public final int offset;

    public WrappedLineLocation(String text, int offset) {
        this.text = text;
        this.offset = offset;
    }
}
