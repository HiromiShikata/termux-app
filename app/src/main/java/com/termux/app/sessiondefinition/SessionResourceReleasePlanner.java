package com.termux.app.sessiondefinition;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Selects the sessions that must hold no runtime resources. A hidden session is reconnected by no
 * scan, is rendered for nobody and is not counted anywhere, so it must own neither a shell process
 * nor a terminal emulator nor the scrollback buffer the emulator carries, whatever its process
 * state. A session whose process has exited and which is neither the current session nor displayed
 * is in the same position for as long as it stays undisplayed. Both keep their row in the session
 * list, and unhiding or reopening the row recreates the runtime, so the selection is reversible and
 * is driven only by the current hidden and displayed state.
 */
public final class SessionResourceReleasePlanner {

    public static final class CandidateSession {

        private final String name;
        private final boolean running;
        private final boolean current;
        private final boolean displayed;
        private final boolean hidden;

        public CandidateSession(@Nullable String name, boolean running, boolean current,
                                boolean displayed, boolean hidden) {
            this.name = name;
            this.running = running;
            this.current = current;
            this.displayed = displayed;
            this.hidden = hidden;
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
