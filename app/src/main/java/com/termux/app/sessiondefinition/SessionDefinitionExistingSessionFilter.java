package com.termux.app.sessiondefinition;

import androidx.annotation.NonNull;

import com.termux.shared.termux.settings.preferences.HiddenSessionNameMatcher;
import com.termux.shared.termux.settings.preferences.UserRemovedSessionHideWindow;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SessionDefinitionExistingSessionFilter {

    private SessionDefinitionExistingSessionFilter() {
    }

    public static List<SessionDefinitionPlannedSession> selectSessionsToCreate(
        @NonNull List<SessionDefinitionPlannedSession> plannedSessions,
        @NonNull Set<String> existingSessionNames,
        @NonNull Set<String> hiddenSessionNames,
        @NonNull Map<String, Long> userRemovedAtMillisBySessionName,
        long nowMillis) {
        Set<String> seenNames = new HashSet<>(existingSessionNames);
        List<SessionDefinitionPlannedSession> sessionsToCreate = new ArrayList<>();
        for (SessionDefinitionPlannedSession plannedSession : plannedSessions) {
            if (HiddenSessionNameMatcher.matchesAHiddenSession(plannedSession.getName(), hiddenSessionNames)) {
                continue;
            }
            if (UserRemovedSessionHideWindow.hidesSession(
                    userRemovedAtMillisBySessionName.get(plannedSession.getName()), nowMillis)) {
                continue;
            }
            if (seenNames.add(plannedSession.getName())) {
                sessionsToCreate.add(plannedSession);
            }
        }
        return sessionsToCreate;
    }
}
