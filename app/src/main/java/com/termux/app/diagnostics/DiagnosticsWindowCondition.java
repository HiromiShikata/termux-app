package com.termux.app.diagnostics;

import androidx.annotation.NonNull;

public final class DiagnosticsWindowCondition {

    public static final DiagnosticsWindowCondition UNMEASURED = new DiagnosticsWindowCondition(false,
        DiagnosticsDrawTime.NEVER_DRAWN, DiagnosticsDrawTime.NEVER_DRAWN, "", false, false);

    private final boolean mMeasured;

    @NonNull
    private final DiagnosticsDrawTime mWindowDrawTime;

    @NonNull
    private final DiagnosticsDrawTime mTerminalDrawTime;

    @NonNull
    private final String mWindowVisibility;

    private final boolean mAttachedToWindow;

    private final boolean mHasWindowFocus;

    private DiagnosticsWindowCondition(boolean measured,
                                       @NonNull DiagnosticsDrawTime windowDrawTime,
                                       @NonNull DiagnosticsDrawTime terminalDrawTime,
                                       @NonNull String windowVisibility,
                                       boolean attachedToWindow,
                                       boolean hasWindowFocus) {
        mMeasured = measured;
        mWindowDrawTime = windowDrawTime;
        mTerminalDrawTime = terminalDrawTime;
        mWindowVisibility = windowVisibility;
        mAttachedToWindow = attachedToWindow;
        mHasWindowFocus = hasWindowFocus;
    }

    @NonNull
    public static DiagnosticsWindowCondition measured(
            @NonNull DiagnosticsDrawTime windowDrawTime,
            @NonNull DiagnosticsDrawTime terminalDrawTime,
            @NonNull String windowVisibility,
            boolean attachedToWindow,
            boolean hasWindowFocus) {
        return new DiagnosticsWindowCondition(true, windowDrawTime, terminalDrawTime, windowVisibility,
            attachedToWindow, hasWindowFocus);
    }

    @NonNull
    public DiagnosticsDrawTime getWindowDrawTime() {
        return mWindowDrawTime;
    }

    public boolean wasMeasured() {
        return mMeasured;
    }

    @NonNull
    public DiagnosticsDrawTime getTerminalDrawTime() {
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
