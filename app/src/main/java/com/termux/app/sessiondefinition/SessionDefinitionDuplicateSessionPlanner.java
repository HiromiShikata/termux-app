package com.termux.app.sessiondefinition;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SessionDefinitionDuplicateSessionPlanner {

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
    public List<Integer> planDuplicateSessionIndexesToRemove(@NonNull List<LiveSession> liveSessions) {
        Map<String, Integer> keptIndexByName = new HashMap<>();
        List<Integer> indexesToRemove = new ArrayList<>();
        for (int index = 0; index < liveSessions.size(); index++) {
            LiveSession liveSession = liveSessions.get(index);
            String name = liveSession.getName();
            if (name == null) {
                continue;
            }
            Integer keptIndex = keptIndexByName.get(name);
            if (keptIndex == null) {
                keptIndexByName.put(name, index);
                continue;
            }
            if (liveSession.isRunning() && !liveSessions.get(keptIndex).isRunning()) {
                indexesToRemove.add(keptIndex);
                keptIndexByName.put(name, index);
            } else {
                indexesToRemove.add(index);
            }
        }
        return indexesToRemove;
    }
}
