package com.termux.app.sessiondefinition;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class SessionResourceReleasePlanner {

    public static final class CandidateSession {

        private final String name;
        private final boolean running;
        private final boolean current;
        private final boolean displayed;
        private final boolean hidden;
        private final boolean underAClosedProject;

        public CandidateSession(@Nullable String name, boolean running, boolean current,
                                boolean displayed, boolean hidden, boolean underAClosedProject) {
            this.name = name;
            this.running = running;
            this.current = current;
            this.displayed = displayed;
            this.hidden = hidden;
            this.underAClosedProject = underAClosedProject;
        }

        @Nullable
        public String getName() {
            return name;
        }

        public boolean isRunning() {
            return running;
        }

        public boolean isCurrent() {
            return current;
        }

        public boolean isDisplayed() {
            return displayed;
        }

        public boolean isHidden() {
            return hidden;
        }

        public boolean isUnderAClosedProject() {
            return underAClosedProject;
        }

        boolean mustReleaseRuntimeResources() {
            if (name == null) {
                return false;
            }
            if (current) {
                return false;
            }
            if (hidden) {
                return true;
            }
            if (underAClosedProject) {
                return true;
            }
            return hasExitedWhileOutOfSight();
        }

        private boolean hasExitedWhileOutOfSight() {
            return !running && !displayed;
        }
    }

    @NonNull
    public List<String> planSessionNamesToRelease(@NonNull List<CandidateSession> candidateSessions) {
        List<String> sessionNamesToRelease = new ArrayList<>();
        for (CandidateSession candidateSession : candidateSessions) {
            if (candidateSession == null) {
                continue;
            }
            if (candidateSession.mustReleaseRuntimeResources()) {
                sessionNamesToRelease.add(candidateSession.getName());
            }
        }
        return sessionNamesToRelease;
    }
}
