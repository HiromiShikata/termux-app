package com.termux.app.diagnostics;

import androidx.annotation.NonNull;

/**
 * What the sessions the application has already replaced had outstanding at the moment they were
 * replaced. The per-session delivery record lives on the terminal session and the automatic
 * reconnect builds a replacement rather than restarting the dead one, so without this the evidence
 * of an episode disappears exactly when the application recovers from it.
 */
public final class DiagnosticsReplacedSessionShellInput {

    private final int mSessionsReplacedWithInputUndelivered;

    private final int mSessionsReplacedAfterTheWriterStopped;

    private final long mWorstUndeliveredBytes;

    @NonNull
    private final String mWorstUndeliveredSessionName;

    @NonNull
    private final String mLastWriterStopSessionName;

    @NonNull
    private final String mLastWriterStopReason;

    public DiagnosticsReplacedSessionShellInput(int sessionsReplacedWithInputUndelivered,
                                                int sessionsReplacedAfterTheWriterStopped,
                                                long worstUndeliveredBytes,
                                                @NonNull String worstUndeliveredSessionName,
                                                @NonNull String lastWriterStopSessionName,
                                                @NonNull String lastWriterStopReason) {
        mSessionsReplacedWithInputUndelivered = sessionsReplacedWithInputUndelivered;
        mSessionsReplacedAfterTheWriterStopped = sessionsReplacedAfterTheWriterStopped;
        mWorstUndeliveredBytes = worstUndeliveredBytes;
        mWorstUndeliveredSessionName = worstUndeliveredSessionName;
        mLastWriterStopSessionName = lastWriterStopSessionName;
        mLastWriterStopReason = lastWriterStopReason;
    }

    public int getSessionsReplacedWithInputUndelivered() {
        return mSessionsReplacedWithInputUndelivered;
    }

    public int getSessionsReplacedAfterTheWriterStopped() {
        return mSessionsReplacedAfterTheWriterStopped;
    }

    public long getWorstUndeliveredBytes() {
        return mWorstUndeliveredBytes;
    }

    @NonNull
    public String getWorstUndeliveredSessionName() {
        return mWorstUndeliveredSessionName;
    }

    @NonNull
    public String getLastWriterStopSessionName() {
        return mLastWriterStopSessionName;
    }

    @NonNull
    public String getLastWriterStopReason() {
        return mLastWriterStopReason;
    }
}
