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

        public CandidateSession(String name, boolean running) {
            this.name = name;
            this.running = running;
        }

        public String getName() {
            return name;
        }

        public boolean isRunning() {
            return running;
        }
    }

    @NonNull
    public List<String> planSessionNamesToReconnect(@NonNull List<CandidateSession> candidateSessions,
                                                    @Nullable String autosshCommandTemplate) {
        List<String> sessionNamesToReconnect = new ArrayList<>();
        for (CandidateSession candidateSession : candidateSessions) {
            if (candidateSession == null || candidateSession.isRunning()) {
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
