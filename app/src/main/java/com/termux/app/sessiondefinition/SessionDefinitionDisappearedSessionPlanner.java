package com.termux.app.sessiondefinition;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public final class SessionDefinitionDisappearedSessionPlanner {

    public static final class LiveSession {

        private final String name;
        private final boolean running;

        public LiveSession(String name, boolean running) {
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
    public List<String> planSessionNamesToRemove(@NonNull List<SessionDefinitionEntry> currentEntries,
                                                 @NonNull Set<String> alwaysPresentSessionNames,
                                                 @NonNull List<LiveSession> liveSessions) {
        return Collections.emptyList();
    }
}
