package com.termux.app.diagnostics;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class ReplacedSessionShellInputRecorder {

    private int mSessionsReplacedWithInputUndelivered;

    private long mWorstUndeliveredBytes;

    @NonNull
    private String mWorstUndeliveredSessionName = "";

    @NonNull
    private String mLastWriterStopSessionName = "";

    @NonNull
    private String mLastWriterStopReason = "";

    public synchronized void recordSessionBeingReplaced(@Nullable String sessionName,
                                                        long undeliveredBytes,
                                                        boolean writerRunning,
                                                        @Nullable String writerStoppedReason) {
        String name = sessionName == null ? "" : sessionName;
        if (undeliveredBytes <= 0 && writerRunning) {
            return;
        }
        mSessionsReplacedWithInputUndelivered++;
        if (undeliveredBytes > mWorstUndeliveredBytes) {
            mWorstUndeliveredBytes = undeliveredBytes;
            mWorstUndeliveredSessionName = name;
        }
        if (!writerRunning) {
            mLastWriterStopSessionName = name;
            mLastWriterStopReason = writerStoppedReason == null ? "" : writerStoppedReason;
        }
    }

    @NonNull
    public synchronized DiagnosticsReplacedSessionShellInput snapshot() {
        return new DiagnosticsReplacedSessionShellInput(mSessionsReplacedWithInputUndelivered,
            mWorstUndeliveredBytes, mWorstUndeliveredSessionName,
            mLastWriterStopSessionName, mLastWriterStopReason);
    }
}
