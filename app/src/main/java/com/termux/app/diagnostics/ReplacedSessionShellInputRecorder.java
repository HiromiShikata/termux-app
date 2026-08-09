package com.termux.app.diagnostics;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class ReplacedSessionShellInputRecorder {

    static final int SESSION_NAME_CHARACTER_LIMIT = 64;

    private int mSessionsReplacedWithInputUndelivered;

    private int mSessionsReplacedAfterTheWriterStopped;

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
        String name = shortenedSessionName(sessionName);
        if (undeliveredBytes > 0) {
            mSessionsReplacedWithInputUndelivered++;
        }
        if (undeliveredBytes > mWorstUndeliveredBytes) {
            mWorstUndeliveredBytes = undeliveredBytes;
            mWorstUndeliveredSessionName = name;
        }
        if (!writerRunning) {
            mSessionsReplacedAfterTheWriterStopped++;
            mLastWriterStopSessionName = name;
            mLastWriterStopReason = writerStoppedReason == null ? "" : writerStoppedReason;
        }
    }

    @NonNull
    private static String shortenedSessionName(@Nullable String sessionName) {
        if (sessionName == null) {
            return "";
        }
        if (sessionName.length() <= SESSION_NAME_CHARACTER_LIMIT) {
            return sessionName;
        }
        return sessionName.substring(0, SESSION_NAME_CHARACTER_LIMIT);
    }

    @NonNull
    public synchronized DiagnosticsReplacedSessionShellInput snapshot() {
        return new DiagnosticsReplacedSessionShellInput(mSessionsReplacedWithInputUndelivered,
            mSessionsReplacedAfterTheWriterStopped,
            mWorstUndeliveredBytes, mWorstUndeliveredSessionName,
            mLastWriterStopSessionName, mLastWriterStopReason);
    }
}
