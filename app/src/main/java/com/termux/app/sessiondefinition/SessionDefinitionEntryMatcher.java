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

        for (SessionDefinitionEntry entry : entries) {
            if (entry.getUrls().contains(sessionName)) {
                return entry;
            }
        }
        return null;
    }

    @Nullable
    public String findTitleForSessionName(@NonNull List<SessionDefinitionEntry> entries,
                                          @Nullable String sessionName) {
        SessionDefinitionEntry entry = findEntryForSessionName(entries, sessionName);
        if (entry == null) {
            return null;
        }
        return entry.getTitleForUrl(sessionName);
    }
}
