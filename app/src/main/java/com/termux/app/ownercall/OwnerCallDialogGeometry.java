package com.termux.app.ownercall;

import androidx.annotation.NonNull;

public final class OwnerCallDialogGeometry {

    public static final int SCREEN_HEIGHT_DIVISOR = 4;
    public static final int VISIBLE_TERMINAL_ROWS_BELOW = 5;

    private final int mWidthPixels;
    private final int mHeightPixels;
    private final int mLeftMarginPixels;
    private final int mBottomMarginPixels;
    private final int mMinBottomMarginPixels;
    private final int mMaxBottomMarginPixels;

    private OwnerCallDialogGeometry(int widthPixels, int heightPixels, int leftMarginPixels,
                                    int bottomMarginPixels, int minBottomMarginPixels,
                                    int maxBottomMarginPixels) {
        mWidthPixels = widthPixels;
        mHeightPixels = heightPixels;
        mLeftMarginPixels = leftMarginPixels;
        mBottomMarginPixels = bottomMarginPixels;
        mMinBottomMarginPixels = minBottomMarginPixels;
        mMaxBottomMarginPixels = maxBottomMarginPixels;
    }

    @NonNull
    public static OwnerCallDialogGeometry resolve(int screenHeightPixels, int terminalAreaLeftPixels,
                                                  int terminalAreaWidthPixels,
                                                  int terminalAreaBottomInsetPixels,
                                                  int terminalRowHeightPixels) {
        int width = Math.max(0, terminalAreaWidthPixels);
        int height = Math.max(0, screenHeightPixels) / SCREEN_HEIGHT_DIVISOR;
        int leftMargin = Math.max(0, terminalAreaLeftPixels);
        int inset = Math.max(0, terminalAreaBottomInsetPixels);
        int defaultBottomMargin = inset
            + Math.max(0, terminalRowHeightPixels) * VISIBLE_TERMINAL_ROWS_BELOW;
        int minBottomMargin = inset;
        int maxBottomMargin = Math.max(minBottomMargin,
            Math.max(0, screenHeightPixels) - height);
        return new OwnerCallDialogGeometry(width, height, leftMargin, defaultBottomMargin,
            minBottomMargin, maxBottomMargin);
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

    public int getMinBottomMarginPixels() {
        return mMinBottomMarginPixels;
    }

    public int getMaxBottomMarginPixels() {
        return mMaxBottomMarginPixels;
    }

    @NonNull
    public OwnerCallDialogGeometry withBottomMargin(int bottomMarginPixels) {
        return new OwnerCallDialogGeometry(mWidthPixels, mHeightPixels, mLeftMarginPixels,
            bottomMarginPixels, mMinBottomMarginPixels, mMaxBottomMarginPixels);
    }
}
