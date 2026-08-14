package com.termux.app.diagnostics;

public final class ScrollWithoutDrawEpisodeRecorder {

    public static final long UNDRAWN_AFTER_SCROLL_THRESHOLD_MILLIS = 2000L;

    private boolean mHasRecordedEpisode;

    private long mRecordedTerminalDrawAtMillis;

    public synchronized boolean recordEpisode(long lastScrollStepAtMillis,
                                              long lastTerminalDrawAtMillis) {
        if (lastScrollStepAtMillis - lastTerminalDrawAtMillis
                < UNDRAWN_AFTER_SCROLL_THRESHOLD_MILLIS) {
            return false;
        }
        if (mHasRecordedEpisode && mRecordedTerminalDrawAtMillis == lastTerminalDrawAtMillis) {
            return false;
        }
        mHasRecordedEpisode = true;
        mRecordedTerminalDrawAtMillis = lastTerminalDrawAtMillis;
        return true;
    }
}
