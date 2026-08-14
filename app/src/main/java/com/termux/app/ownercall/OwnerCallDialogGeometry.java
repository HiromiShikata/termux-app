package com.termux.app.ownercall;

import androidx.annotation.NonNull;

public final class OwnerCallDialogGeometry {

    public static final int SCREEN_HEIGHT_DIVISOR = 4;
    public static final int VISIBLE_TERMINAL_ROWS_BELOW = 5;

    private final int mWidthPixels;
    private final int mHeightPixels;
    private final int mLeftMarginPixels;
    private final int mBottomMarginPixels;

    private OwnerCallDialogGeometry(int widthPixels, int heightPixels, int leftMarginPixels,
                                    int bottomMarginPixels) {
        mWidthPixels = widthPixels;
        mHeightPixels = heightPixels;
        mLeftMarginPixels = leftMarginPixels;
        mBottomMarginPixels = bottomMarginPixels;
    }

    @NonNull
    public static OwnerCallDialogGeometry resolve(int screenHeightPixels, int terminalAreaLeftPixels,
                                                  int terminalAreaWidthPixels,
                                                  int terminalAreaBottomInsetPixels,
                                                  int terminalRowHeightPixels) {
        return new OwnerCallDialogGeometry(
            Math.max(0, terminalAreaWidthPixels),
            Math.max(0, screenHeightPixels) / SCREEN_HEIGHT_DIVISOR,
            Math.max(0, terminalAreaLeftPixels),
            Math.max(0, terminalAreaBottomInsetPixels)
                + Math.max(0, terminalRowHeightPixels) * VISIBLE_TERMINAL_ROWS_BELOW);
    }

    public int getWidthPixels() {
        return mWidthPixels;
    }

    public int getHeightPixels() {
        return mHeightPixels;
    }

    public int getLeftMarginPixels() {
        return mLeftMarginPixels;
    }

    public int getBottomMarginPixels() {
        return mBottomMarginPixels;
    }
}
