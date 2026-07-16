package com.termux.app.terminal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.terminal.TerminalSession;

public final class SessionReplyTimeRecorder {

    @NonNull
    private final SessionNewActivityStore mStore;

    public SessionReplyTimeRecorder(@NonNull SessionNewActivityStore store) {
        mStore = store;
    }

    public boolean recordReplyOnSubmit(@Nullable TerminalSession session, long submitTimeMillis) {
        if (session == null || session.mSessionName == null) {
            return false;
        }
        mStore.recordUserInput(session.mSessionName, submitTimeMillis);
        return true;
    }
}
