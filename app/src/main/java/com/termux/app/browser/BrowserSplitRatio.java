package com.termux.app.browser;

public final class BrowserSplitRatio {

    public static final float MIN = 2f / 3f;

    public static final float MAX = 1f;

    private BrowserSplitRatio() {
    }

    public static float clamp(float ratio) {
        return Math.max(MIN, Math.min(MAX, ratio));
    }
}
