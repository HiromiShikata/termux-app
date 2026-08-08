package com.termux.app.diagnostics;

import androidx.annotation.NonNull;

public final class AppVersionChangeHolder {

    @NonNull
    private static volatile DiagnosticsVersionChange sVersionChange =
        DiagnosticsVersionChange.sameVersionAsThePreviousLaunch();

    private AppVersionChangeHolder() {
    }

    public static void set(@NonNull DiagnosticsVersionChange versionChange) {
        sVersionChange = versionChange;
    }

    @NonNull
    public static DiagnosticsVersionChange getInstance() {
        return sVersionChange;
    }
}
