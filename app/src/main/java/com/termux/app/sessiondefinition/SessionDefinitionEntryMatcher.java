package com.termux.app.sessiondefinition;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

public final class SessionDefinitionEntryMatcher {

    @Nullable
    public SessionDefinitionEntry findEntryForSessionName(@NonNull List<SessionDefinitionEntry> entries,
                                                          @Nullable String sessionName) {
        if (sessionName == null || sessionName.isEmpty()) {
            return null;
        }

        SessionDefinitionEntry bestMatch = null;
        int bestMatchLength = -1;
        for (SessionDefinitionEntry entry : entries) {
            String entrySessionName = entry.getSessionName();
            if (!matches(sessionName, entrySessionName)) {
                continue;
            }
            int length = entrySessionName.length();
            if (length > bestMatchLength) {
                bestMatch = entry;
                bestMatchLength = length;
            }
        }
        return bestMatch;
    }

    private boolean matches(@NonNull String sessionName, @Nullable String entrySessionName) {
        if (entrySessionName == null || entrySessionName.isEmpty()) {
            return false;
        }
        return sessionName.equals(entrySessionName)
            || sessionName.startsWith(entrySessionName + " ");
    }
}
