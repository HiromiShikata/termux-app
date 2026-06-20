package com.termux.app.terminal;

public final class SessionPickerScrollOffset {

    private SessionPickerScrollOffset() {
    }

    public static int targetScrollY(int highlightedLineTop, int highlightedLineBottom,
                                    int viewportHeight, int contentHeight, int currentScrollY) {
        int maxScrollY = Math.max(0, contentHeight - viewportHeight);
        if (maxScrollY == 0) {
            return 0;
        }
        int highlightedLineHeight = highlightedLineBottom - highlightedLineTop;
        int centeredScrollY = highlightedLineTop - (viewportHeight - highlightedLineHeight) / 2;
        int desiredScrollY;
        if (highlightedLineHeight >= viewportHeight) {
            desiredScrollY = highlightedLineTop;
        } else if (highlightedLineTop < currentScrollY) {
            desiredScrollY = centeredScrollY;
        } else if (highlightedLineBottom > currentScrollY + viewportHeight) {
            desiredScrollY = centeredScrollY;
        } else {
            desiredScrollY = currentScrollY;
        }
        return clamp(desiredScrollY, 0, maxScrollY);
    }

    private static int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }
}
