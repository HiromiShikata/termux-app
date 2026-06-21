package com.termux.app.browser;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.app.sessiondefinition.SessionDefinitionEntry;
import com.termux.app.sessiondefinition.SessionDefinitionEntryMatcher;

import java.util.List;

public final class BrowserProjectNameResolver {

    public interface SessionDefinitionEntriesSupplier {
        @NonNull
        List<SessionDefinitionEntry> getSessionDefinitionEntries();
    }

    private final SessionDefinitionEntriesSupplier entriesSupplier;

    private final SessionDefinitionEntryMatcher matcher = new SessionDefinitionEntryMatcher();

    public BrowserProjectNameResolver(@NonNull SessionDefinitionEntriesSupplier entriesSupplier) {
        this.entriesSupplier = entriesSupplier;
    }

    @Nullable
    public String resolveProjectName(@Nullable String sessionName) {
        if (sessionName == null || sessionName.isEmpty()) {
            return null;
        }
        List<SessionDefinitionEntry> entries = entriesSupplier.getSessionDefinitionEntries();
        if (entries.isEmpty()) {
            return null;
        }
        return matcher.findGroupLabelForSessionName(entries, sessionName);
    }
}
