package com.termux.app.terminal;

public final class SessionInfoHorizontalBounds {

    public static final int MATCH_PARENT_WIDTH = -1;

    private SessionInfoHorizontalBounds() {
    }

    public static int resolveWidthPixels(boolean landscape, int browserColumnWidthPixels) {
        if (!landscape) {
            return MATCH_PARENT_WIDTH;
        }
        if (browserColumnWidthPixels <= 0) {
            return MATCH_PARENT_WIDTH;
        }
        return browserColumnWidthPixels;
    }
}
