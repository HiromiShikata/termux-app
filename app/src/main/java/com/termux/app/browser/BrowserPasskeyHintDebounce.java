package com.termux.app.browser;

public final class BrowserPasskeyHintDebounce {

    public static final long DEFAULT_MIN_INTERVAL_MS = 10_000L;

    private final long minIntervalMs;

    private boolean hasShown;

    private long lastShownAtMs;

    public BrowserPasskeyHintDebounce() {
        this(DEFAULT_MIN_INTERVAL_MS);
    }

    public BrowserPasskeyHintDebounce(long minIntervalMs) {
        this.minIntervalMs = minIntervalMs;
    }

    public boolean shouldShow(long nowMs) {
        if (hasShown && nowMs - lastShownAtMs < minIntervalMs) {
            return false;
        }
        hasShown = true;
        lastShownAtMs = nowMs;
        return true;
    }
}
