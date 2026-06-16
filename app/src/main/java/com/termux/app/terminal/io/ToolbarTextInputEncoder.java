package com.termux.app.terminal.io;

public final class ToolbarTextInputEncoder {

    private ToolbarTextInputEncoder() {
    }

    public static String textToSend(String textInput, boolean submitWhenEmpty) {
        if (textInput.length() == 0 && submitWhenEmpty) return "\r";
        return textInput;
    }

    public static boolean hasContentToSend(String textInput, boolean submitWhenEmpty) {
        return textToSend(textInput, submitWhenEmpty).length() > 0;
    }
}
