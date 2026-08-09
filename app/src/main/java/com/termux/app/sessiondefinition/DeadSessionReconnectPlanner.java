package com.termux.app.sessiondefinition;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.app.terminal.session.FinishedSessionEnterAction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public final class DeadSessionReconnectPlanner {

    public static final class CandidateSession {

        private final String name;
        private final boolean running;
        private final boolean current;
        private final boolean hung;
        private final Long lastOutTimeMillis;
        private final boolean reconnecting;
        private final boolean unableToReceiveInputLongEnough;

        public CandidateSession(String name, boolean running) {
            this(name, running, false, false, null);
        }

        public CandidateSession(String name, boolean running, boolean current, boolean hung,
                                @Nullable Long lastOutTimeMillis) {
            this(name, running, current, hung, lastOutTimeMillis, false);
        }

        public CandidateSession(String name, boolean running, boolean current, boolean hung,
                                @Nullable Long lastOutTimeMillis, boolean reconnecting) {
            this(name, running, current, hung, lastOutTimeMillis, reconnecting, false);
        }

        public CandidateSession(String name, boolean running, boolean current, boolean hung,
                                @Nullable Long lastOutTimeMillis, boolean reconnecting,
                                boolean unableToReceiveInputLongEnough) {
            this.name = name;
            this.running = running;
            this.current = current;
            this.hung = running && hung;
            this.lastOutTimeMillis = lastOutTimeMillis;
            this.reconnecting = reconnecting;
            this.unableToReceiveInputLongEnough = unableToReceiveInputLongEnough;
        }

        public boolean isUnableToReceiveInputLongEnough() {
            return unableToReceiveInputLongEnough;
        }

        public String getName() {
            return name;
        }

        public boolean isRunning() {
            return running;
        }

        public boolean isCurrent() {
            return current;
        }

        public boolean isHung() {
            return hung;
        }

        @Nullable
        public Long getLastOutTimeMillis() {
            return lastOutTimeMillis;
        }

        public boolean isReconnecting() {
            return reconnecting;
        }

        boolean isDeadProcessReconnectCandidate() {
            return !running && !reconnecting;
        }

        boolean isDetachedInputReconnectCandidate() {
            return unableToReceiveInputLongEnough && !reconnecting;
        }

        boolean isHungAliveReconnectCandidate() {
            return !current && hung && !reconnecting;
        }
    }

    public static final int UNLIMITED = Integer.MAX_VALUE;

    @NonNull
    public List<String> planSessionNamesToReconnect(@NonNull List<CandidateSession> candidateSessions,
                                                    @Nullable String autosshCommandTemplate) {
        return planSessionNamesToReconnect(candidateSessions, autosshCommandTemplate, UNLIMITED);
    }

    @NonNull
    public List<String> planSessionNamesToReconnect(@NonNull List<CandidateSession> candidateSessions,
                                                    @Nullable String autosshCommandTemplate,
                                                    int maxSessionsToReconnect) {
        return planSessionNamesToReconnect(candidateSessions, autosshCommandTemplate, maxSessionsToReconnect,
            Collections.emptySet());
    }

    /**
     * Plans which stale definition-backed sessions to reconnect, capped at {@code
     * maxSessionsToReconnect}. A session whose shell process has exited is planned whether or not it is
     * the one currently displayed in the terminal view; a session that is still running but has gone
     * silent is planned only while it is not the displayed one, because it may be the session the owner
     * is typing into and it has no in-place replacement. The proactive background reconnect passes
     * {@link #UNLIMITED} so every currently-stale session is planned and none is left to go 30+ minutes
     * stale; the caller then spaces the resulting reconnects out in time so a large batch never fires
     * simultaneously. A finite cap is still honored for callers that want to bound a single pass. Dead
     * processes are preferred over merely hung ones, and hung sessions are ordered oldest-output-first
     * so the most stale session is reconnected first when a finite cap is reached.
     */
    @NonNull
    public List<String> planSessionNamesToReconnect(@NonNull List<CandidateSession> candidateSessions,
                                                    @Nullable String autosshCommandTemplate,
                                                    int maxSessionsToReconnect,
                                                    @NonNull Set<String> userRemovedSessionNames) {
        List<String> sessionNamesToReconnect = new ArrayList<>();
        for (PlannedSessionReconnect plannedReconnect : planReconnects(candidateSessions,
            autosshCommandTemplate, maxSessionsToReconnect, userRemovedSessionNames)) {
            sessionNamesToReconnect.add(plannedReconnect.getSessionName());
        }
        return sessionNamesToReconnect;
    }

    @NonNull
    public List<PlannedSessionReconnect> planReconnects(@NonNull List<CandidateSession> candidateSessions,
                                                        @Nullable String autosshCommandTemplate,
                                                        int maxSessionsToReconnect,
                                                        @NonNull Set<String> userRemovedSessionNames) {
        List<PlannedSessionReconnect> plannedReconnects = new ArrayList<>();
        if (maxSessionsToReconnect <= 0) {
            return plannedReconnects;
        }
        List<CandidateSession> hungAliveCandidates = new ArrayList<>();
        for (CandidateSession candidateSession : candidateSessions) {
            if (candidateSession == null) {
                continue;
            }
            if (candidateSession.isDeadProcessReconnectCandidate()
                || candidateSession.isDetachedInputReconnectCandidate()) {
                addIfReconnectable(candidateSession, reasonThatPlanned(candidateSession),
                    autosshCommandTemplate, userRemovedSessionNames, plannedReconnects);
                if (plannedReconnects.size() >= maxSessionsToReconnect) {
                    return plannedReconnects;
                }
            } else if (candidateSession.isHungAliveReconnectCandidate()) {
                hungAliveCandidates.add(candidateSession);
            }
        }
        hungAliveCandidates.sort(OLDEST_OUT_FIRST);
        for (CandidateSession hungAliveCandidate : hungAliveCandidates) {
            addIfReconnectable(hungAliveCandidate,
                SessionReconnectReason.SILENT_FOR_LONGER_THAN_THE_STALENESS_THRESHOLD,
                autosshCommandTemplate, userRemovedSessionNames, plannedReconnects);
            if (plannedReconnects.size() >= maxSessionsToReconnect) {
                return plannedReconnects;
            }
        }
        return plannedReconnects;
    }

    @NonNull
    private static SessionReconnectReason reasonThatPlanned(@NonNull CandidateSession candidateSession) {
        return candidateSession.isDeadProcessReconnectCandidate()
            ? SessionReconnectReason.SHELL_PROCESS_GONE_AT_THE_BACKGROUND_SCAN
            : SessionReconnectReason.INPUT_NO_LONGER_REACHES_THE_PROGRAM;
    }

    private static void addIfReconnectable(@NonNull CandidateSession candidateSession,
                                           @NonNull SessionReconnectReason reason,
                                           @Nullable String autosshCommandTemplate,
                                           @NonNull Set<String> userRemovedSessionNames,
                                           @NonNull List<PlannedSessionReconnect> plannedReconnects) {
        FinishedSessionEnterAction action =
            FinishedSessionEnterAction.decide(candidateSession.getName(), autosshCommandTemplate,
                userRemovedSessionNames);
        if (action.isReconnect()) {
            plannedReconnects.add(new PlannedSessionReconnect(candidateSession.getName(), reason));
        }
    }

    private static final Comparator<CandidateSession> OLDEST_OUT_FIRST =
        Comparator.comparing(CandidateSession::getLastOutTimeMillis,
            Comparator.nullsFirst(Comparator.naturalOrder()));
}
