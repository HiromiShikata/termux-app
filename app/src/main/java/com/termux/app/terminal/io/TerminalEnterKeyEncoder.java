package com.termux.app.terminal.io;

import android.view.KeyEvent;

import com.termux.terminal.KeyHandler;

public final class TerminalEnterKeyEncoder {

    private TerminalEnterKeyEncoder() {
    }

    public static String enterSequence(boolean cursorKeysApplicationMode, boolean keypadApplicationMode) {
        return KeyHandler.getCode(KeyEvent.KEYCODE_ENTER, 0, cursorKeysApplicationMode, keypadApplicationMode);
    }
}
