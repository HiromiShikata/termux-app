package com.termux.app.sessiondefinition;

import androidx.annotation.NonNull;

import com.termux.view.url.BrowsableUrlDetector;

import java.util.ArrayList;
import java.util.LinkedHashSet;
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
        Set<String> definedSessionNames = collectDefinedSessionNames(currentEntries);
        Set<String> protectedSessionNames = trimmedNonEmpty(alwaysPresentSessionNames);
        List<String> sessionNamesToRemove = new ArrayList<>();
        for (LiveSession liveSession : liveSessions) {
            String sessionName = liveSession.getName();
            if (sessionName == null || sessionName.isEmpty()) {
                continue;
            }
            if (!BrowsableUrlDetector.isLikelyBrowsableUrl(sessionName)) {
                continue;
            }
            if (definedSessionNames.contains(sessionName)) {
                continue;
            }
            if (protectedSessionNames.contains(sessionName.trim())) {
                continue;
            }
            if (liveSession.isRunning()) {
                continue;
            }
            sessionNamesToRemove.add(sessionName);
        }
        return sessionNamesToRemove;
    }

    @NonNull
    private static Set<String> collectDefinedSessionNames(@NonNull List<SessionDefinitionEntry> entries) {
        Set<String> definedSessionNames = new LinkedHashSet<>();
        for (SessionDefinitionEntry entry : entries) {
            definedSessionNames.addAll(entry.getUrls());
        }
        return definedSessionNames;
    }

    @NonNull
    private static Set<String> trimmedNonEmpty(@NonNull Set<String> names) {
        Set<String> trimmed = new LinkedHashSet<>();
        for (String name : names) {
            if (name == null) {
                continue;
            }
            String trimmedName = name.trim();
            if (!trimmedName.isEmpty()) {
                trimmed.add(trimmedName);
            }
        }
        return trimmed;
    }
}
