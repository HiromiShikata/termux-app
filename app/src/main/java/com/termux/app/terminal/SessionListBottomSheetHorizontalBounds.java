package com.termux.app.terminal;

public final class SessionListBottomSheetHorizontalBounds {

    public static final int MATCH_PARENT_WIDTH = -1;

    private SessionListBottomSheetHorizontalBounds() {
    }

    public static int resolveWidthPixels(boolean landscape, int containerWidthPixels) {
        if (!landscape) {
            return MATCH_PARENT_WIDTH;
        }
        if (containerWidthPixels <= 0) {
            return MATCH_PARENT_WIDTH;
        }
        return containerWidthPixels / 2;
    }

    public static boolean alignToEnd(boolean landscape) {
        return landscape;
    }
}
