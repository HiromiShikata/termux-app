package com.termux.app.sessiondefinition;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

public final class DefinitionBackedSessionCounter {

    private final SessionDefinitionPlanner sessionDefinitionPlanner = new SessionDefinitionPlanner();

    public int countDefinitionBackedSessions(@NonNull List<String> sessionNames,
                                             @Nullable String autosshCommandTemplate) {
        int count = 0;
        for (String sessionName : sessionNames) {
            if (isDefinitionBacked(sessionName, autosshCommandTemplate)) {
                count++;
            }
        }
        return count;
    }

    public boolean isDefinitionBacked(@Nullable String sessionName, @Nullable String autosshCommandTemplate) {
        if (sessionName == null || sessionName.trim().isEmpty()) {
            return false;
        }
        return sessionDefinitionPlanner.planNamedSession(sessionName, autosshCommandTemplate).hasCommand();
    }
}
