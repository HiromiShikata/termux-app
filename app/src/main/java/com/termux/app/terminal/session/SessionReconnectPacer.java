package com.termux.app.terminal.session;

import androidx.annotation.NonNull;

import com.termux.terminal.TerminalSession;

import java.util.Iterator;
import java.util.LinkedHashSet;

public final class SessionReconnectPacer {

    public static final long MAIN_THREAD_FRAME_YIELD_INTERVAL_MILLIS = 16L;

    public interface MainThreadMessagePoster {
        void postToMainThreadDelayed(@NonNull Runnable runnable, long delayMillis);
    }

    public interface SessionStillInTheReconnectList {
        boolean stillContains(@NonNull TerminalSession session);
    }

    public interface SessionReconnectAction {
        void reconnectSession(@NonNull TerminalSession session);
    }

    private final MainThreadMessagePoster mainThreadMessagePoster;

    private final SessionStillInTheReconnectList sessionStillInTheReconnectList;

    private final SessionReconnectAction sessionReconnectAction;

    private final LinkedHashSet<TerminalSession> pendingSessions = new LinkedHashSet<>();

    private boolean unitMessagePosted;

    public SessionReconnectPacer(
        @NonNull MainThreadMessagePoster mainThreadMessagePoster,
        @NonNull SessionStillInTheReconnectList sessionStillInTheReconnectList,
        @NonNull SessionReconnectAction sessionReconnectAction) {
        this.mainThreadMessagePoster = mainThreadMessagePoster;
        this.sessionStillInTheReconnectList = sessionStillInTheReconnectList;
        this.sessionReconnectAction = sessionReconnectAction;
    }

    public void enqueueSession(@NonNull TerminalSession session) {
        if (!pendingSessions.add(session)) return;
        postNextUnitMessageIfIdle();
    }

    private void postNextUnitMessageIfIdle() {
        if (unitMessagePosted) return;
        if (pendingSessions.isEmpty()) return;
        unitMessagePosted = true;
        mainThreadMessagePoster.postToMainThreadDelayed(
            this::runNextUnit, MAIN_THREAD_FRAME_YIELD_INTERVAL_MILLIS);
    }

    private void runNextUnit() {
        unitMessagePosted = false;
        try {
            TerminalSession session = pollNextPendingSession();
            if (session == null) return;
            if (!sessionStillInTheReconnectList.stillContains(session)) return;
            sessionReconnectAction.reconnectSession(session);
        } finally {
            postNextUnitMessageIfIdle();
        }
    }

    private TerminalSession pollNextPendingSession() {
        Iterator<TerminalSession> pendingSessionIterator = pendingSessions.iterator();
        if (!pendingSessionIterator.hasNext()) return null;
        TerminalSession nextPendingSession = pendingSessionIterator.next();
        pendingSessionIterator.remove();
        return nextPendingSession;
    }
}
