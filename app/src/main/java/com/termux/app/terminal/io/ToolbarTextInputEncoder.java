package com.termux.app.terminal.io;

public final class ToolbarTextInputEncoder {

    private ToolbarTextInputEncoder() {
    }

    public static String textToSend(String textInput, boolean submitWhenEmpty) {
        if (textInput.length() == 0 && submitWhenEmpty) return "\r";
        return textInput;
    }
}
