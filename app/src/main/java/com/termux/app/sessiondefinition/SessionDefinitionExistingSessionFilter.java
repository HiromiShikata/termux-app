package com.termux.app.sessiondefinition;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class SessionDefinitionExistingSessionFilter {

    private SessionDefinitionExistingSessionFilter() {
    }

    public static List<SessionDefinitionPlannedSession> selectSessionsToCreate(
        List<SessionDefinitionPlannedSession> plannedSessions, Set<String> existingSessionNames) {
        Set<String> seenNames = new HashSet<>(existingSessionNames);
        List<SessionDefinitionPlannedSession> sessionsToCreate = new ArrayList<>();
        for (SessionDefinitionPlannedSession plannedSession : plannedSessions) {
            if (seenNames.add(plannedSession.getName())) {
                sessionsToCreate.add(plannedSession);
            }
        }
        return sessionsToCreate;
    }
}
