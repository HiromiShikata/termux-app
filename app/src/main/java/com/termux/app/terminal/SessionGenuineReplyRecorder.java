package com.termux.app.terminal;

import androidx.annotation.Nullable;

import com.termux.terminal.TerminalSession;

public final class SessionGenuineReplyRecorder {

    public interface AnsweredOwnerCallDeleter {
        void deleteAnsweredOwnerCallsForSession(String sessionName);
    }

    public interface SessionNameOverlayUpdater {
        void updateSessionNameOverlay();
    }

    private final SessionNewActivityStore mStore;
    private final AnsweredOwnerCallDeleter mOwnerCallDeleter;
    private final SessionNameOverlayUpdater mOverlayUpdater;

    public SessionGenuineReplyRecorder(
            SessionNewActivityStore store,
            AnsweredOwnerCallDeleter ownerCallDeleter,
            SessionNameOverlayUpdater overlayUpdater) {
        this.mStore = store;
        this.mOwnerCallDeleter = ownerCallDeleter;
        this.mOverlayUpdater = overlayUpdater;
    }

    public boolean recordReply(
            @Nullable TerminalSession session, long timeMillis, boolean isCurrentSession) {
        if (!new SessionReplyTimeRecorder(mStore).recordReplyOnSubmit(session, timeMillis)) {
            return false;
        }
        mOwnerCallDeleter.deleteAnsweredOwnerCallsForSession(session.mSessionName);
        if (isCurrentSession) {
            mOverlayUpdater.updateSessionNameOverlay();
        }
        return true;
    }
}
