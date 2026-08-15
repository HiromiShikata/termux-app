package com.termux.app.ownercall;

import androidx.annotation.NonNull;

public final class OwnerCallDialogGeometry {

    public static final int SCREEN_HEIGHT_DIVISOR = 4;
    public static final int VISIBLE_TERMINAL_ROWS_BELOW = 5;
    public static final int MINIMUM_WIDTH_DIVISOR = 4;
    public static final int MINIMUM_HEIGHT_DIVISOR = 10;

    @NonNull
    private final OwnerCallDialogPlacement mPlacement;

    @NonNull
    private final OwnerCallDialogPlacement mDefaultPlacement;

    private final int mAvailableWidthPixels;
    private final int mAvailableHeightPixels;

    private OwnerCallDialogGeometry(@NonNull OwnerCallDialogPlacement placement,
                                    @NonNull OwnerCallDialogPlacement defaultPlacement,
                                    int availableWidthPixels, int availableHeightPixels) {
        mPlacement = placement;
        mDefaultPlacement = defaultPlacement;
        mAvailableWidthPixels = availableWidthPixels;
        mAvailableHeightPixels = availableHeightPixels;
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
        OwnerCallDialogPlacement defaultPlacement =
            new OwnerCallDialogPlacement(leftMargin, defaultBottomMargin, width, height);
        return new OwnerCallDialogGeometry(defaultPlacement, defaultPlacement, leftMargin + width,
            Math.max(0, screenHeightPixels));
    }

    public int getWidthPixels() {
        return mPlacement.getWidthPixels();
    }

    public int getHeightPixels() {
        return mPlacement.getHeightPixels();
    }

    public int getLeftMarginPixels() {
        return mPlacement.getLeftMarginPixels();
    }

    public int getBottomMarginPixels() {
        return mPlacement.getBottomMarginPixels();
    }

    @NonNull
    public OwnerCallDialogPlacement getDefaultPlacement() {
        return mDefaultPlacement;
    }

    public int getAvailableWidthPixels() {
        return mAvailableWidthPixels;
    }

    public int getAvailableHeightPixels() {
        return mAvailableHeightPixels;
    }

    public int getMinimumWidthPixels() {
        return mAvailableWidthPixels / MINIMUM_WIDTH_DIVISOR;
    }

    public int getMinimumHeightPixels() {
        return mAvailableHeightPixels / MINIMUM_HEIGHT_DIVISOR;
    }

    @NonNull
    public OwnerCallDialogGeometry withPlacement(@NonNull OwnerCallDialogPlacement placement) {
        return new OwnerCallDialogGeometry(placement, mDefaultPlacement, mAvailableWidthPixels,
            mAvailableHeightPixels);
    }
}
