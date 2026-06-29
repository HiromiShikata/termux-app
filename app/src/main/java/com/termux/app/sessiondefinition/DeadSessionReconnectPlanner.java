package com.termux.app.sessiondefinition;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.app.terminal.session.FinishedSessionEnterAction;

import java.util.ArrayList;
import java.util.List;

public final class DeadSessionReconnectPlanner {

    public static final class CandidateSession {

        private final String name;
        private final boolean running;
        private final boolean current;
        private final boolean hungConnection;

        public CandidateSession(String name, boolean running) {
            this(name, running, false, false);
        }

        public CandidateSession(String name, boolean running, boolean current, boolean hungConnection) {
            this.name = name;
            this.running = running;
            this.current = current;
            this.hungConnection = hungConnection;
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

        public boolean isHungConnection() {
            return hungConnection;
        }

        boolean needsReconnect() {
            if (current) {
                return false;
            }
            return !running || hungConnection;
        }
    }

    @NonNull
    public List<String> planSessionNamesToReconnect(@NonNull List<CandidateSession> candidateSessions,
                                                    @Nullable String autosshCommandTemplate) {
        List<String> sessionNamesToReconnect = new ArrayList<>();
        for (CandidateSession candidateSession : candidateSessions) {
            if (candidateSession == null || !candidateSession.needsReconnect()) {
                continue;
            }
            FinishedSessionEnterAction action =
                FinishedSessionEnterAction.decide(candidateSession.getName(), autosshCommandTemplate);
            if (action.isReconnect()) {
                sessionNamesToReconnect.add(candidateSession.getName());
            }
        }
        return sessionNamesToReconnect;
    }
}
