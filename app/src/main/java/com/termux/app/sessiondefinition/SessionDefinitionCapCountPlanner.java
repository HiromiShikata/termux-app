package com.termux.app.sessiondefinition;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.app.terminal.session.FinishedSessionEnterAction;

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

        boolean countsTowardCap(@Nullable String autosshCommandTemplate) {
            if (running) {
                return true;
            }
            return FinishedSessionEnterAction.decide(name, autosshCommandTemplate).getKind()
                == FinishedSessionEnterAction.Kind.RECONNECT;
        }
    }

    public int countSessionsTowardCap(@NonNull List<CountedSession> countedSessions,
                                      @Nullable String autosshCommandTemplate) {
        int count = 0;
        for (CountedSession countedSession : countedSessions) {
            if (countedSession == null) {
                continue;
            }
            if (countedSession.countsTowardCap(autosshCommandTemplate)) {
                count++;
            }
        }
        return count;
    }
}
