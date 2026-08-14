package com.termux.app.ownercall;

import androidx.annotation.NonNull;

public final class OwnerCallDialogGeometry {

    public static final int SCREEN_HEIGHT_DIVISOR = 4;
    public static final int VISIBLE_TERMINAL_ROWS_BELOW = 5;

    private OwnerCallDialogGeometry() {
    }

    @NonNull
    public static OwnerCallDialogGeometry resolve(int screenHeightPixels, int terminalAreaLeftPixels,
                                                  int terminalAreaWidthPixels,
                                                  int terminalAreaBottomInsetPixels,
                                                  int terminalRowHeightPixels) {
        throw new UnsupportedOperationException();
    }

    public int getWidthPixels() {
        throw new UnsupportedOperationException();
    }

    public int getHeightPixels() {
        throw new UnsupportedOperationException();
    }

    public int getLeftMarginPixels() {
        throw new UnsupportedOperationException();
    }

    public int getBottomMarginPixels() {
        throw new UnsupportedOperationException();
    }
}
