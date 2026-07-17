package com.termux.app.sessiondefinition;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class SessionDefinitionAlwaysPresentPriorityPlanner {

    @NonNull
    private final SessionDefinitionPlanner sessionDefinitionPlanner = new SessionDefinitionPlanner();

    @NonNull
    public List<SessionDefinitionPlannedSession> planPrioritizedSessions(
            @NonNull List<SessionDefinitionPlannedSession> definitionSessions,
            @NonNull Collection<String> alwaysPresentSessionNames,
            String commandTemplate) {
        List<SessionDefinitionPlannedSession> prioritizedSessions = new ArrayList<>();
        Set<String> placedNames = new LinkedHashSet<>();
        for (String alwaysPresentSessionName : alwaysPresentSessionNames) {
            if (alwaysPresentSessionName == null) {
                continue;
            }
            String trimmedName = alwaysPresentSessionName.trim();
            if (trimmedName.isEmpty()) {
                continue;
            }
            if (!placedNames.add(trimmedName)) {
                continue;
            }
            SessionDefinitionPlannedSession plannedSession =
                sessionDefinitionPlanner.planNamedSession(trimmedName, commandTemplate);
            prioritizedSessions.add(plannedSession);
        }
        for (SessionDefinitionPlannedSession definitionSession : definitionSessions) {
            if (!placedNames.add(definitionSession.getName())) {
                continue;
            }
            prioritizedSessions.add(definitionSession);
        }
        return prioritizedSessions;
    }
}
