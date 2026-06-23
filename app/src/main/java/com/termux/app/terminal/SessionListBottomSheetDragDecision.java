package com.termux.app.terminal;

public final class SessionListBottomSheetDragDecision {

    static final int DEFAULT_HEIGHT_SCREEN_FRACTION_DENOMINATOR = 2;
    static final int MIN_HEIGHT_SCREEN_FRACTION_DENOMINATOR = 4;
    static final int MAX_HEIGHT_SCREEN_PERCENT_NUMERATOR = 85;
    static final int MAX_HEIGHT_SCREEN_PERCENT_DENOMINATOR = 100;
    static final int DISMISS_BELOW_MIN_FRACTION_DENOMINATOR = 3;
    static final int DISMISS_FLING_MIN_VELOCITY_PIXELS_PER_SECOND = 800;

    private SessionListBottomSheetDragDecision() {
    }

    static int computeDefaultHeight(int screenHeightPixels) {
        return screenHeightPixels / DEFAULT_HEIGHT_SCREEN_FRACTION_DENOMINATOR;
    }

    static int computeMinHeight(int screenHeightPixels) {
        return screenHeightPixels / MIN_HEIGHT_SCREEN_FRACTION_DENOMINATOR;
    }

    static int computeMaxHeight(int screenHeightPixels) {
        return screenHeightPixels * MAX_HEIGHT_SCREEN_PERCENT_NUMERATOR / MAX_HEIGHT_SCREEN_PERCENT_DENOMINATOR;
    }

    static int resolveDragHeight(float downwardDragPixels, int startHeightPixels, int minHeightPixels,
                                 int maxHeightPixels) {
        int proposedHeight = Math.round(startHeightPixels - downwardDragPixels);
        if (proposedHeight < minHeightPixels) {
            return minHeightPixels;
        }
        if (proposedHeight > maxHeightPixels) {
            return maxHeightPixels;
        }
        return proposedHeight;
    }

    static float resolveDragTranslation(float downwardDragPixels, int startHeightPixels, int minHeightPixels) {
        float belowMinPixels = minHeightPixels - (startHeightPixels - downwardDragPixels);
        return belowMinPixels > 0f ? belowMinPixels : 0f;
    }

    static boolean shouldDismissAfterDrag(float downwardDragPixels, int startHeightPixels, int minHeightPixels,
                                          float verticalVelocityPixelsPerSecond) {
        if (verticalVelocityPixelsPerSecond >= DISMISS_FLING_MIN_VELOCITY_PIXELS_PER_SECOND) {
            return true;
        }
        float belowMinPixels = minHeightPixels - (startHeightPixels - downwardDragPixels);
        return belowMinPixels >= minHeightPixels / DISMISS_BELOW_MIN_FRACTION_DENOMINATOR;
    }
}
