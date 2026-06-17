package com.termux.app.browser;

public final class BrowserPageLoadProgressState {

    private static final int MIN_PROGRESS = 0;

    private static final int COMPLETE_PROGRESS = 100;

    private final boolean visible;

    private final int progress;

    private BrowserPageLoadProgressState(boolean visible, int progress) {
        this.visible = visible;
        this.progress = progress;
    }

    public static BrowserPageLoadProgressState forProgress(int reportedProgress) {
        int clampedProgress = Math.max(MIN_PROGRESS, Math.min(COMPLETE_PROGRESS, reportedProgress));
        return new BrowserPageLoadProgressState(clampedProgress < COMPLETE_PROGRESS, clampedProgress);
    }

    public boolean isVisible() {
        return visible;
    }

    public int getProgress() {
        return progress;
    }
}
