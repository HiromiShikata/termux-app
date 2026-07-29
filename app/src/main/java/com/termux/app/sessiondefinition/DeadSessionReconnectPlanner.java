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

        public CandidateSession(String name, boolean running) {
            this(name, running, false, false, null);
        }

        public CandidateSession(String name, boolean running, boolean current, boolean hung,
                                @Nullable Long lastOutTimeMillis) {
            this(name, running, current, hung, lastOutTimeMillis, false);
        }

        public CandidateSession(String name, boolean running, boolean current, boolean hung,
                                @Nullable Long lastOutTimeMillis, boolean reconnecting) {
            this.name = name;
            this.running = running;
            this.current = current;
            this.hung = hung;
            this.lastOutTimeMillis = lastOutTimeMillis;
            this.reconnecting = reconnecting;
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

        /**
         * A session whose reconnect is still in flight is not selected again: its replacement was
         * created moments ago, owns no shell process until its emulator is initialised and has had no
         * opportunity to render a statusline, so selecting it again on the next scan tears down the
         * reconnect that is still running and repeats without end. The bounded reconnect timeout and
         * retry ladder owns that session until it settles, and clears the in-flight state on both the
         * success and the exhausted-retry path, so the exclusion always ends.
         *
         * <p>Hung means alive but no longer producing output, so it describes a running session only.
         * A candidate that is at once not running and hung carries a recorded output time that cannot
         * belong to it — the signature of a replacement that inherited the time of the session it
         * replaced and can never refresh it — and reconnecting on that basis is what leaves the
         * selection without a reachable exit, so such a candidate is not selected either.
         */
        boolean isDeadProcessReconnectCandidate() {
            return !current && !running && !hung && !reconnecting;
        }

        boolean isHungAliveReconnectCandidate() {
            return !current && running && hung && !reconnecting;
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
     * Plans which stale non-current definition-backed sessions to reconnect, capped at {@code
     * maxSessionsToReconnect}. The proactive background reconnect passes {@link #UNLIMITED} so every
     * currently-stale non-current session is planned and none is left to go 30+ minutes stale; the
     * caller then spaces the resulting reconnects out in time so a large batch never fires
     * simultaneously. A finite cap is still honored for callers that want to bound a single pass. Dead
     * processes are preferred over merely hung ones, and hung sessions are ordered oldest-output-first
     * so the most stale session is reconnected first when a finite cap is reached.
     */
    @NonNull
    public List<String> planSessionNamesToReconnect(@NonNull List<CandidateSession> candidateSessions,
                                                    @Nullable String autosshCommandTemplate,
                                                    int maxSessionsToReconnect,
                                                    @NonNull Set<String> userRemovedSessionNames) {
        if (maxSessionsToReconnect <= 0) {
            return new ArrayList<>();
        }
        List<String> sessionNamesToReconnect = new ArrayList<>();
        List<CandidateSession> hungAliveCandidates = new ArrayList<>();
        for (CandidateSession candidateSession : candidateSessions) {
            if (candidateSession == null) {
                continue;
            }
            if (candidateSession.isDeadProcessReconnectCandidate()) {
                addIfReconnectable(candidateSession, autosshCommandTemplate, userRemovedSessionNames,
                    sessionNamesToReconnect);
                if (sessionNamesToReconnect.size() >= maxSessionsToReconnect) {
                    return sessionNamesToReconnect;
                }
            } else if (candidateSession.isHungAliveReconnectCandidate()) {
                hungAliveCandidates.add(candidateSession);
            }
        }
        hungAliveCandidates.sort(OLDEST_OUT_FIRST);
        for (CandidateSession hungAliveCandidate : hungAliveCandidates) {
            addIfReconnectable(hungAliveCandidate, autosshCommandTemplate, userRemovedSessionNames,
                sessionNamesToReconnect);
            if (sessionNamesToReconnect.size() >= maxSessionsToReconnect) {
                return sessionNamesToReconnect;
            }
        }
        return sessionNamesToReconnect;
    }

    private static void addIfReconnectable(@NonNull CandidateSession candidateSession,
                                           @Nullable String autosshCommandTemplate,
                                           @NonNull Set<String> userRemovedSessionNames,
                                           @NonNull List<String> sessionNamesToReconnect) {
        FinishedSessionEnterAction action =
            FinishedSessionEnterAction.decide(candidateSession.getName(), autosshCommandTemplate,
                userRemovedSessionNames);
        if (action.isReconnect()) {
            sessionNamesToReconnect.add(candidateSession.getName());
        }
    }

    private static final Comparator<CandidateSession> OLDEST_OUT_FIRST =
        Comparator.comparing(CandidateSession::getLastOutTimeMillis,
            Comparator.nullsFirst(Comparator.naturalOrder()));
}
