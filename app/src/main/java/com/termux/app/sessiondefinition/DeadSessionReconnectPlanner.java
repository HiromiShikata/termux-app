package com.termux.app.sessiondefinition;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.app.terminal.session.FinishedSessionEnterAction;

import java.util.ArrayList;
import java.util.List;

public final class DeadSessionReconnectPlanner {

    public static final int MAX_HUNG_RECONNECTS_PER_TICK = 1;

    public static final class CandidateSession {

        private final String name;
        private final boolean running;
        private final boolean current;
        private final boolean hung;
        private final Long lastOutTimeMillis;

        public CandidateSession(String name, boolean running) {
            this(name, running, false, false, null);
        }

        public CandidateSession(String name, boolean running, boolean current, boolean hung,
                                @Nullable Long lastOutTimeMillis) {
            this.name = name;
            this.running = running;
            this.current = current;
            this.hung = hung;
            this.lastOutTimeMillis = lastOutTimeMillis;
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

        boolean isDeadProcessReconnectCandidate() {
            return !current && !running;
        }

        boolean isHungAliveReconnectCandidate() {
            return !current && running && hung;
        }
    }

    @NonNull
    public List<String> planSessionNamesToReconnect(@NonNull List<CandidateSession> candidateSessions,
                                                    @Nullable String autosshCommandTemplate) {
        List<String> sessionNamesToReconnect = new ArrayList<>();
        CandidateSession oldestHungAliveCandidate = null;
        for (CandidateSession candidateSession : candidateSessions) {
            if (candidateSession == null) {
                continue;
            }
            if (candidateSession.isDeadProcessReconnectCandidate()) {
                addIfReconnectable(candidateSession, autosshCommandTemplate, sessionNamesToReconnect);
            } else if (candidateSession.isHungAliveReconnectCandidate()) {
                oldestHungAliveCandidate = olderOut(oldestHungAliveCandidate, candidateSession);
            }
        }
        if (MAX_HUNG_RECONNECTS_PER_TICK > 0 && oldestHungAliveCandidate != null) {
            addIfReconnectable(oldestHungAliveCandidate, autosshCommandTemplate, sessionNamesToReconnect);
        }
        return sessionNamesToReconnect;
    }

    private static void addIfReconnectable(@NonNull CandidateSession candidateSession,
                                           @Nullable String autosshCommandTemplate,
                                           @NonNull List<String> sessionNamesToReconnect) {
        FinishedSessionEnterAction action =
            FinishedSessionEnterAction.decide(candidateSession.getName(), autosshCommandTemplate);
        if (action.isReconnect()) {
            sessionNamesToReconnect.add(candidateSession.getName());
        }
    }

    @NonNull
    private static CandidateSession olderOut(@Nullable CandidateSession current,
                                             @NonNull CandidateSession candidate) {
        if (current == null) {
            return candidate;
        }
        Long currentOut = current.getLastOutTimeMillis();
        Long candidateOut = candidate.getLastOutTimeMillis();
        if (candidateOut == null) {
            return current;
        }
        if (currentOut == null) {
            return candidate;
        }
        return candidateOut < currentOut ? candidate : current;
    }
}
