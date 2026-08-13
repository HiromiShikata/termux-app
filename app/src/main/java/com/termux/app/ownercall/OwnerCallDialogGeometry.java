package com.termux.app.ownercall;

import androidx.annotation.NonNull;

public final class OwnerCallDialogGeometry {

    public static final int SCREEN_HEIGHT_DIVISOR = 4;
    public static final int VISIBLE_TERMINAL_ROWS_BELOW = 5;

    private final int widthPixels;
    private final int heightPixels;
    private final int leftMarginPixels;
    private final int bottomMarginPixels;

    private OwnerCallDialogGeometry(int widthPixels, int heightPixels, int leftMarginPixels,
                                    int bottomMarginPixels) {
        this.widthPixels = widthPixels;
        this.heightPixels = heightPixels;
        this.leftMarginPixels = leftMarginPixels;
        this.bottomMarginPixels = bottomMarginPixels;
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
        return widthPixels;
    }

    public int getHeightPixels() {
        return heightPixels;
    }

    public int getLeftMarginPixels() {
        return leftMarginPixels;
    }

    public int getBottomMarginPixels() {
        return bottomMarginPixels;
    }
}
