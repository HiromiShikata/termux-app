package com.termux.app.diagnostics;

public final class ScrollWithoutDrawEpisode {

    private final long mScrolledAtMillis;

    private final long mUndrawnForMillis;

    public ScrollWithoutDrawEpisode(long scrolledAtMillis, long undrawnForMillis) {
        mScrolledAtMillis = scrolledAtMillis;
        mUndrawnForMillis = undrawnForMillis;
    }

    public long getScrolledAtMillis() {
        return mScrolledAtMillis;
    }

    public long getUndrawnForMillis() {
        return mUndrawnForMillis;
    }
}
