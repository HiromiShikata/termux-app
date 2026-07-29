package com.termux.app.sessiondefinition;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.shared.termux.settings.preferences.HiddenSessionNameMatcher;

import java.util.List;
import java.util.Set;

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

        boolean countsTowardCap(@NonNull Set<String> hiddenSessionNames) {
            return running && !HiddenSessionNameMatcher.matchesAHiddenSession(name, hiddenSessionNames);
        }
    }

    public int countSessionsTowardCap(@NonNull List<CountedSession> countedSessions,
                                      @NonNull Set<String> hiddenSessionNames) {
        int count = 0;
        for (CountedSession countedSession : countedSessions) {
            if (countedSession == null) {
                continue;
            }
            if (countedSession.countsTowardCap(hiddenSessionNames)) {
                count++;
            }
        }
        return count;
    }
}
