package com.termux.app.diagnostics;

import androidx.annotation.NonNull;

public final class DiagnosticsTerminalDrawTime {

    public static final DiagnosticsTerminalDrawTime NEVER_DRAWN =
        new DiagnosticsTerminalDrawTime(false, 0L);

    private final boolean mHasDrawn;

    private final long mMillisSinceLastDraw;

    private DiagnosticsTerminalDrawTime(boolean hasDrawn, long millisSinceLastDraw) {
        mHasDrawn = hasDrawn;
        mMillisSinceLastDraw = millisSinceLastDraw;
    }

    @NonNull
    public static DiagnosticsTerminalDrawTime drawnMillisAgo(long millisSinceLastDraw) {
        return new DiagnosticsTerminalDrawTime(true, millisSinceLastDraw);
    }

    public boolean hasDrawn() {
        return mHasDrawn;
    }

    public long getMillisSinceLastDraw() {
        return mMillisSinceLastDraw;
    }
}
