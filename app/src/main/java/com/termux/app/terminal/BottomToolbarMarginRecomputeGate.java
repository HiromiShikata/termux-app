package com.termux.app.terminal;

public class BottomToolbarMarginRecomputeGate {

    private Boolean lastKeyboardVisible;

    public boolean shouldRecomputeMargin(boolean keyboardVisible) {
        boolean changed = lastKeyboardVisible == null || lastKeyboardVisible != keyboardVisible;
        lastKeyboardVisible = keyboardVisible;
        return changed;
    }

}
