package com.termux.app.diagnostics;

import androidx.annotation.NonNull;

public final class AppVersionChangeRecorder {

    public static final int NO_VERSION_CODE_RECORDED = -1;

    @NonNull
    public DiagnosticsVersionChange versionChangeOfThisLaunch(int versionCodeOfThePreviousLaunch,
                                                              int runningVersionCode) {
        if (versionCodeOfThePreviousLaunch == NO_VERSION_CODE_RECORDED) {
            return DiagnosticsVersionChange.firstLaunchAfterInstallation();
        }
        if (versionCodeOfThePreviousLaunch == runningVersionCode) {
            return DiagnosticsVersionChange.sameVersionAsThePreviousLaunch();
        }
        return DiagnosticsVersionChange.firstLaunchAfterReplacing(versionCodeOfThePreviousLaunch);
    }
}
