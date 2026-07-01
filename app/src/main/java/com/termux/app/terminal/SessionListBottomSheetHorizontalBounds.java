package com.termux.app.terminal;

public final class SessionListBottomSheetHorizontalBounds {

    public static final int MATCH_PARENT_WIDTH = -1;

    private SessionListBottomSheetHorizontalBounds() {
    }

    public static int resolveWidthPixels(boolean landscape, int containerWidthPixels) {
        return MATCH_PARENT_WIDTH;
    }

    public static boolean alignToEnd(boolean landscape) {
        return false;
    }
}
