package com.termux.app.diagnostics;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.view.scroll.TerminalScrollEvent;

public final class DiagnosticsScrollGestureRouting {

    private final boolean mHasEmulator;
    private final boolean mMouseTrackingActive;
    private final boolean mAlternateScreenActive;

    @Nullable
    private final TerminalScrollEvent mFingerGestureDestination;

    private DiagnosticsScrollGestureRouting(boolean hasEmulator, boolean mouseTrackingActive,
                                            boolean alternateScreenActive,
                                            @Nullable TerminalScrollEvent fingerGestureDestination) {
        mHasEmulator = hasEmulator;
        mMouseTrackingActive = mouseTrackingActive;
        mAlternateScreenActive = alternateScreenActive;
        mFingerGestureDestination = fingerGestureDestination;
    }

    @NonNull
    public static DiagnosticsScrollGestureRouting ofEmulatorState(boolean mouseTrackingActive,
                                                                  boolean alternateScreenActive) {
        return new DiagnosticsScrollGestureRouting(true, mouseTrackingActive, alternateScreenActive,
            TerminalScrollEvent.ofEmulatorState(mouseTrackingActive, alternateScreenActive, false));
    }

    @NonNull
    public static DiagnosticsScrollGestureRouting withoutAnEmulator() {
        return new DiagnosticsScrollGestureRouting(false, false, false, null);
    }

    public boolean hasEmulator() {
        return mHasEmulator;
    }

    public boolean isMouseTrackingActive() {
        return mMouseTrackingActive;
    }

    public boolean isAlternateScreenActive() {
        return mAlternateScreenActive;
    }

    @Nullable
    public TerminalScrollEvent getFingerGestureDestination() {
        return mFingerGestureDestination;
    }

    @NonNull
    public String getReportLabel() {
        if (!mHasEmulator) return "no emulator";
        return destinationLabel()
            + " (mouse tracking " + onOrOff(mMouseTrackingActive)
            + ", alternate screen " + onOrOff(mAlternateScreenActive) + ")";
    }

    @NonNull
    private String destinationLabel() {
        switch (mFingerGestureDestination) {
            case MOUSE_WHEEL:
                return "the shell as a mouse wheel";
            case ARROW_KEY:
                return "the shell as arrow keys";
            case LOCAL_SCROLLBACK:
                return "the view's own scrollback";
        }
        throw new IllegalStateException("unhandled scroll gesture destination " + mFingerGestureDestination);
    }

    @NonNull
    private String onOrOff(boolean value) {
        return value ? "on" : "off";
    }
}
