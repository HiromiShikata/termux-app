package com.termux.app.terminal.session;

import androidx.annotation.NonNull;

import com.termux.app.sessiondefinition.SessionReconnectReason;
import com.termux.terminal.TerminalSession;

import java.util.AbstractMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

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
        void recordReconnectCost(@NonNull SessionReconnectReason reason, long elapsedNanos,
                                 int sessionsStillQueued);
    }

    public interface SharedCreationWorkBatch {
        void begin();

        void end();
    }

    private final MainThreadMessagePoster mainThreadMessagePoster;

    private final SessionStillInTheReconnectList sessionStillInTheReconnectList;

    private final SessionReconnectAction sessionReconnectAction;

    private final ElapsedNanosClock elapsedNanosClock;

    private final ReconnectCostRecorder reconnectCostRecorder;

    private final SharedCreationWorkBatch sharedCreationWorkBatch;

    private final LinkedHashMap<TerminalSession, SessionReconnectReason> pendingSessions =
        new LinkedHashMap<>();

    private boolean unitMessagePosted;

    private boolean sharedCreationWorkBatchIsOpen;

    public SessionReconnectPacer(
        @NonNull MainThreadMessagePoster mainThreadMessagePoster,
        @NonNull SessionStillInTheReconnectList sessionStillInTheReconnectList,
        @NonNull SessionReconnectAction sessionReconnectAction,
        @NonNull ElapsedNanosClock elapsedNanosClock,
        @NonNull ReconnectCostRecorder reconnectCostRecorder,
        @NonNull SharedCreationWorkBatch sharedCreationWorkBatch) {
        this.mainThreadMessagePoster = mainThreadMessagePoster;
        this.sessionStillInTheReconnectList = sessionStillInTheReconnectList;
        this.sessionReconnectAction = sessionReconnectAction;
        this.elapsedNanosClock = elapsedNanosClock;
        this.reconnectCostRecorder = reconnectCostRecorder;
        this.sharedCreationWorkBatch = sharedCreationWorkBatch;
    }

    public void enqueueSession(@NonNull TerminalSession session,
                               @NonNull SessionReconnectReason reason) {
        if (pendingSessions.containsKey(session)) return;
        pendingSessions.put(session, reason);
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
            Map.Entry<TerminalSession, SessionReconnectReason> pendingReconnect = pollNextPendingSession();
            if (pendingReconnect == null) return;
            TerminalSession session = pendingReconnect.getKey();
            if (!sessionStillInTheReconnectList.stillContains(session)) return;
            openSharedCreationWorkBatchUnlessAlreadyOpen();
            long reconnectStartedAtNanos = elapsedNanosClock.elapsedNanos();
            try {
                sessionReconnectAction.reconnectSession(session);
            } finally {
                reconnectCostRecorder.recordReconnectCost(pendingReconnect.getValue(),
                    elapsedNanosClock.elapsedNanos() - reconnectStartedAtNanos, pendingSessions.size());
            }
        } finally {
            closeSharedCreationWorkBatchWhenNothingIsStillQueued();
            postNextUnitMessageIfIdle();
        }
    }

    private void openSharedCreationWorkBatchUnlessAlreadyOpen() {
        if (sharedCreationWorkBatchIsOpen) return;
        sharedCreationWorkBatchIsOpen = true;
        sharedCreationWorkBatch.begin();
    }

    private void closeSharedCreationWorkBatchWhenNothingIsStillQueued() {
        if (!sharedCreationWorkBatchIsOpen) return;
        if (!pendingSessions.isEmpty()) return;
        sharedCreationWorkBatchIsOpen = false;
        sharedCreationWorkBatch.end();
    }

    private Map.Entry<TerminalSession, SessionReconnectReason> pollNextPendingSession() {
        Iterator<Map.Entry<TerminalSession, SessionReconnectReason>> pendingSessionIterator =
            pendingSessions.entrySet().iterator();
        if (!pendingSessionIterator.hasNext()) return null;
        Map.Entry<TerminalSession, SessionReconnectReason> nextPendingReconnect =
            pendingSessionIterator.next();
        SessionReconnectReason reason = nextPendingReconnect.getValue();
        pendingSessionIterator.remove();
        return new AbstractMap.SimpleImmutableEntry<>(nextPendingReconnect.getKey(), reason);
    }
}
