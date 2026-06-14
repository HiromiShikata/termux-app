package com.termux.app.terminal;

public final class SessionListBottomSheetDragDecision {

    static final int DISMISS_DISTANCE_FRACTION_DENOMINATOR = 3;
    static final int DISMISS_FLING_MIN_VELOCITY_PIXELS_PER_SECOND = 800;

    private SessionListBottomSheetDragDecision() {
    }

    static float clampDragTranslation(float rawTranslationPixels, float sheetHeightPixels) {
        if (rawTranslationPixels < 0f) {
            return 0f;
        }
        if (rawTranslationPixels > sheetHeightPixels) {
            return sheetHeightPixels;
        }
        return rawTranslationPixels;
    }

    static boolean shouldDismissAfterDrag(float currentTranslationPixels, float verticalVelocityPixelsPerSecond,
                                          float sheetHeightPixels) {
        if (verticalVelocityPixelsPerSecond >= DISMISS_FLING_MIN_VELOCITY_PIXELS_PER_SECOND) {
            return true;
        }
        return currentTranslationPixels >= sheetHeightPixels / DISMISS_DISTANCE_FRACTION_DENOMINATOR;
    }
}
