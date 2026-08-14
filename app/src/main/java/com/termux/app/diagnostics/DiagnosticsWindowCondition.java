package com.termux.app.diagnostics;

import androidx.annotation.NonNull;

public final class DiagnosticsWindowCondition {

    public static final DiagnosticsWindowCondition UNMEASURED = new DiagnosticsWindowCondition(false,
        DiagnosticsTerminalDrawTime.NEVER_DRAWN, "", false, false);

    private final boolean mMeasured;

    @NonNull
    private final DiagnosticsTerminalDrawTime mTerminalDrawTime;

    @NonNull
    private final String mWindowVisibility;

    private final boolean mAttachedToWindow;

    private final boolean mHasWindowFocus;

    private DiagnosticsWindowCondition(boolean measured,
                                       @NonNull DiagnosticsTerminalDrawTime terminalDrawTime,
                                       @NonNull String windowVisibility,
                                       boolean attachedToWindow,
                                       boolean hasWindowFocus) {
        mMeasured = measured;
        mTerminalDrawTime = terminalDrawTime;
        mWindowVisibility = windowVisibility;
        mAttachedToWindow = attachedToWindow;
        mHasWindowFocus = hasWindowFocus;
    }

    @NonNull
    public static DiagnosticsWindowCondition measured(
            @NonNull DiagnosticsTerminalDrawTime terminalDrawTime,
            @NonNull String windowVisibility,
            boolean attachedToWindow,
            boolean hasWindowFocus) {
        return new DiagnosticsWindowCondition(true, terminalDrawTime, windowVisibility,
            attachedToWindow, hasWindowFocus);
    }

    public boolean wasMeasured() {
        return mMeasured;
    }

    @NonNull
    public DiagnosticsTerminalDrawTime getTerminalDrawTime() {
        return mTerminalDrawTime;
    }

    @NonNull
    public String getWindowVisibility() {
        return mWindowVisibility;
    }

    public boolean isAttachedToWindow() {
        return mAttachedToWindow;
    }

    public boolean hasWindowFocus() {
        return mHasWindowFocus;
    }
}
