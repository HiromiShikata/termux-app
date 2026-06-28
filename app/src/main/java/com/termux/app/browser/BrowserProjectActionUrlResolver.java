package com.termux.app.browser;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.app.sessiondefinition.SessionDefinitionEntry;
import com.termux.app.sessiondefinition.SessionDefinitionEntryMatcher;

import java.util.List;

public final class BrowserProjectActionUrlResolver {

    public interface SessionDefinitionEntriesSupplier {
        @NonNull
        List<SessionDefinitionEntry> getSessionDefinitionEntries();
    }

    private final SessionDefinitionEntriesSupplier entriesSupplier;

    private final SessionDefinitionEntryMatcher matcher = new SessionDefinitionEntryMatcher();

    public BrowserProjectActionUrlResolver(@NonNull SessionDefinitionEntriesSupplier entriesSupplier) {
        this.entriesSupplier = entriesSupplier;
    }

    @NonNull
    public BrowserProjectActionUrls resolveForSessionName(@Nullable String sessionName) {
        List<SessionDefinitionEntry> entries = entriesSupplier.getSessionDefinitionEntries();
        SessionDefinitionEntry matchedEntry = matcher.findEntryForSessionName(entries, sessionName);
        if (matchedEntry == null) {
            return BrowserProjectActionUrls.EMPTY;
        }
        String groupLabel = matchedEntry.getGroupLabel();
        String overviewUrl = null;
        String tdpmConsoleUrl = null;
        String newIssueUrl = null;
        for (SessionDefinitionEntry entry : entries) {
            if (!sameGroup(groupLabel, entry.getGroupLabel())) {
                continue;
            }
            if (overviewUrl == null) overviewUrl = entry.getOverviewUrl();
            if (tdpmConsoleUrl == null) tdpmConsoleUrl = entry.getTdpmConsoleUrl();
            if (newIssueUrl == null) newIssueUrl = entry.getNewIssueUrl();
        }
        return new BrowserProjectActionUrls(overviewUrl, tdpmConsoleUrl, newIssueUrl);
    }

    private static boolean sameGroup(@Nullable String firstGroupLabel, @Nullable String secondGroupLabel) {
        if (firstGroupLabel == null) {
            return secondGroupLabel == null;
        }
        return firstGroupLabel.equals(secondGroupLabel);
    }
}
