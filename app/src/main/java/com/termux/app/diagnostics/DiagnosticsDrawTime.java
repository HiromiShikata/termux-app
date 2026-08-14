package com.termux.app.diagnostics;

import androidx.annotation.NonNull;

public final class DiagnosticsDrawTime {

    public static final DiagnosticsDrawTime NEVER_DRAWN =
        new DiagnosticsDrawTime(false, 0L);

    private final boolean mHasDrawn;

    private final long mMillisSinceLastDraw;

    private DiagnosticsDrawTime(boolean hasDrawn, long millisSinceLastDraw) {
        mHasDrawn = hasDrawn;
        mMillisSinceLastDraw = millisSinceLastDraw;
    }

    @NonNull
    public static DiagnosticsDrawTime drawnMillisAgo(long millisSinceLastDraw) {
        return new DiagnosticsDrawTime(true, millisSinceLastDraw);
    }

    public boolean hasDrawn() {
        return mHasDrawn;
    }

    public long getMillisSinceLastDraw() {
        return mMillisSinceLastDraw;
    }
}
