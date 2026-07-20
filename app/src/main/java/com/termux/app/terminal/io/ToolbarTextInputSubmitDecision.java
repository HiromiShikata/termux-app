package com.termux.app.terminal.io;

import android.view.inputmethod.EditorInfo;

public final class ToolbarTextInputSubmitDecision {

    private ToolbarTextInputSubmitDecision() {
    }

    public static boolean shouldSubmitForEditorAction(int actionId, boolean hasKeyEvent) {
        if (hasKeyEvent) return false;
        return actionId == EditorInfo.IME_ACTION_SEND || actionId == EditorInfo.IME_NULL;
    }
}
