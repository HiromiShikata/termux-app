package com.termux.app.browser;

public final class BrowserSplitRatio {

    public static final float MIN = 0f;

    public static final float MAX = 1f;

    public static final float DEFAULT = 2f / 3f;

    public static final float COLLAPSE_THRESHOLD = 0.05f;

    private BrowserSplitRatio() {
    }

    public static float clamp(float ratio) {
        return Math.max(MIN, Math.min(MAX, ratio));
    }

    public static boolean isCollapsed(float ratio) {
        return clamp(ratio) <= COLLAPSE_THRESHOLD;
    }
}
