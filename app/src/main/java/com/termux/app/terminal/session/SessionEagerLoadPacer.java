package com.termux.app.terminal.session;

import androidx.annotation.NonNull;

import com.termux.terminal.TerminalSession;

import java.util.Iterator;
import java.util.LinkedHashSet;

public final class SessionEagerLoadPacer {

    public static final long MAIN_THREAD_FRAME_YIELD_INTERVAL_MILLIS = 16L;

    public interface MainThreadMessagePoster {
        void postToMainThreadDelayed(@NonNull Runnable runnable, long delayMillis);
    }

    private final MainThreadMessagePoster mainThreadMessagePoster;

    private final SessionEagerLoader.SessionInitializationAction sessionInitializationAction;

    private final LinkedHashSet<TerminalSession> pendingSessions = new LinkedHashSet<>();

    private boolean unitMessagePosted;

    public SessionEagerLoadPacer(
        @NonNull MainThreadMessagePoster mainThreadMessagePoster,
        @NonNull SessionEagerLoader.SessionInitializationAction sessionInitializationAction) {
        this.mainThreadMessagePoster = mainThreadMessagePoster;
        this.sessionInitializationAction = sessionInitializationAction;
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
            if (session != null) sessionInitializationAction.initializeSession(session);
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
