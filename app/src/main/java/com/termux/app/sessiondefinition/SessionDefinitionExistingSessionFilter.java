package com.termux.app.sessiondefinition;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class SessionDefinitionExistingSessionFilter {

    private SessionDefinitionExistingSessionFilter() {
    }

    public static List<SessionDefinitionPlannedSession> selectSessionsToCreate(
        @NonNull List<SessionDefinitionPlannedSession> plannedSessions,
        @NonNull Set<String> existingSessionNames,
        @NonNull Set<String> hiddenSessionNames) {
        Set<String> seenNames = new HashSet<>(existingSessionNames);
        List<SessionDefinitionPlannedSession> sessionsToCreate = new ArrayList<>();
        for (SessionDefinitionPlannedSession plannedSession : plannedSessions) {
            if (HiddenSessionNameMatcher.matchesAHiddenSession(plannedSession.getName(), hiddenSessionNames)) {
                continue;
            }
            if (seenNames.add(plannedSession.getName())) {
                sessionsToCreate.add(plannedSession);
            }
        }
        return sessionsToCreate;
    }
}
