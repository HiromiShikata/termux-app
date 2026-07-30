package com.termux.app.terminal.io;

public final class ToolbarTextInputEncoder {

    private ToolbarTextInputEncoder() {
    }

    public static String textToSend(String textInput) {
        return textInput;
    }

    public static boolean hasContentToSend(String textInput) {
        return textToSend(textInput).length() > 0;
    }
}
