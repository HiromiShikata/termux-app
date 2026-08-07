package com.termux.app.diagnostics;

import androidx.annotation.NonNull;

/**
 * Process-lifetime holder for what the replaced sessions had outstanding, so an episode in which
 * typed input stopped reaching the shell outlives the reconnect that ends it.
 */
public final class ReplacedSessionShellInputRecorderHolder {

    private static final ReplacedSessionShellInputRecorder INSTANCE =
        new ReplacedSessionShellInputRecorder();

    private ReplacedSessionShellInputRecorderHolder() {
    }

    @NonNull
    public static ReplacedSessionShellInputRecorder getInstance() {
        return INSTANCE;
    }
}
