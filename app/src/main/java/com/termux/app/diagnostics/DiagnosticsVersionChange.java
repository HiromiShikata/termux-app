package com.termux.app.diagnostics;

import androidx.annotation.NonNull;

public final class DiagnosticsVersionChange {

    private static final int NO_PREVIOUS_VERSION_CODE = -1;

    private final boolean mFirstLaunchOfThisVersion;
    private final int mPreviousVersionCode;

    @NonNull
    public static DiagnosticsVersionChange sameVersionAsThePreviousLaunch() {
        return new DiagnosticsVersionChange(false, NO_PREVIOUS_VERSION_CODE);
    }

    @NonNull
    public static DiagnosticsVersionChange firstLaunchAfterInstallation() {
        return new DiagnosticsVersionChange(true, NO_PREVIOUS_VERSION_CODE);
    }

    @NonNull
    public static DiagnosticsVersionChange firstLaunchAfterReplacing(int previousVersionCode) {
        return new DiagnosticsVersionChange(true, previousVersionCode);
    }

    private DiagnosticsVersionChange(boolean firstLaunchOfThisVersion, int previousVersionCode) {
        mFirstLaunchOfThisVersion = firstLaunchOfThisVersion;
        mPreviousVersionCode = previousVersionCode;
    }

    public boolean isFirstLaunchOfThisVersion() {
        return mFirstLaunchOfThisVersion;
    }

    public boolean hasPreviousVersionCode() {
        return mPreviousVersionCode != NO_PREVIOUS_VERSION_CODE;
    }

    public int getPreviousVersionCode() {
        return mPreviousVersionCode;
    }
}
