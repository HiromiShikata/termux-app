package com.termux.app.sessiondefinition;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

public final class SessionDefinitionCapCountPlanner {

    public static final class CountedSession {

        private final String name;
        private final boolean running;

        public CountedSession(@Nullable String name, boolean running) {
            this.name = name;
            this.running = running;
        }

        @Nullable
        public String getName() {
            return name;
        }

        public boolean isRunning() {
            return running;
        }

        boolean countsTowardCap() {
            return running;
        }
    }

    public int countSessionsTowardCap(@NonNull List<CountedSession> countedSessions) {
        int count = 0;
        for (CountedSession countedSession : countedSessions) {
            if (countedSession == null) {
                continue;
            }
            if (countedSession.countsTowardCap()) {
                count++;
            }
        }
        return count;
    }
}
