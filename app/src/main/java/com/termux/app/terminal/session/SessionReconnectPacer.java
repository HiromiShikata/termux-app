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

    public interface ElapsedNanosClock {
        long elapsedNanos();
    }

    public interface ReconnectCostRecorder {
        void recordReconnectCost(long elapsedNanos, int sessionsStillQueued);
    }

    private final MainThreadMessagePoster mainThreadMessagePoster;

    private final SessionStillInTheReconnectList sessionStillInTheReconnectList;

    private final SessionReconnectAction sessionReconnectAction;

    private final ElapsedNanosClock elapsedNanosClock;

    private final ReconnectCostRecorder reconnectCostRecorder;

    private final LinkedHashSet<TerminalSession> pendingSessions = new LinkedHashSet<>();

    private boolean unitMessagePosted;

    public SessionReconnectPacer(
        @NonNull MainThreadMessagePoster mainThreadMessagePoster,
        @NonNull SessionStillInTheReconnectList sessionStillInTheReconnectList,
        @NonNull SessionReconnectAction sessionReconnectAction,
        @NonNull ElapsedNanosClock elapsedNanosClock,
        @NonNull ReconnectCostRecorder reconnectCostRecorder) {
        this.mainThreadMessagePoster = mainThreadMessagePoster;
        this.sessionStillInTheReconnectList = sessionStillInTheReconnectList;
        this.sessionReconnectAction = sessionReconnectAction;
        this.elapsedNanosClock = elapsedNanosClock;
        this.reconnectCostRecorder = reconnectCostRecorder;
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
            long reconnectStartedAtNanos = elapsedNanosClock.elapsedNanos();
            try {
                sessionReconnectAction.reconnectSession(session);
            } finally {
                reconnectCostRecorder.recordReconnectCost(
                    elapsedNanosClock.elapsedNanos() - reconnectStartedAtNanos, pendingSessions.size());
            }
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
