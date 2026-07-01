package com.termux.app.browser;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.app.sessiondefinition.DefaultProjectManagerSessionPlanner;
import com.termux.app.sessiondefinition.SessionDefinitionEntry;
import com.termux.app.sessiondefinition.SessionDefinitionEntryMatcher;

import java.util.List;

public final class BrowserProjectActionUrlResolver {

    public interface SessionDefinitionEntriesSupplier {
        @NonNull
        List<SessionDefinitionEntry> getSessionDefinitionEntries();
    }

    private static final String URL_SCHEME_SEPARATOR = "://";

    private final SessionDefinitionEntriesSupplier entriesSupplier;

    private final SessionDefinitionEntryMatcher matcher = new SessionDefinitionEntryMatcher();

    private final DefaultProjectManagerSessionPlanner projectSessionPlanner =
        new DefaultProjectManagerSessionPlanner();

    public BrowserProjectActionUrlResolver(@NonNull SessionDefinitionEntriesSupplier entriesSupplier) {
        this.entriesSupplier = entriesSupplier;
    }

    @NonNull
    public BrowserProjectActionUrls resolveForSessionName(@Nullable String sessionName) {
        List<SessionDefinitionEntry> entries = entriesSupplier.getSessionDefinitionEntries();
        String groupLabel = resolveGroupLabelForSessionName(entries, sessionName);
        if (groupLabel == null) {
            return BrowserProjectActionUrls.EMPTY;
        }
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

    @Nullable
    private String resolveGroupLabelForSessionName(@NonNull List<SessionDefinitionEntry> entries,
                                                   @Nullable String sessionName) {
        SessionDefinitionEntry matchedEntry = matcher.findEntryForSessionName(entries, sessionName);
        if (matchedEntry != null) {
            return matchedEntry.getGroupLabel();
        }
        if (sessionName == null || sessionName.isEmpty() || isUrl(sessionName)) {
            return null;
        }
        return matchProjectSessionNameToGroupLabel(entries, sessionName);
    }

    @Nullable
    private String matchProjectSessionNameToGroupLabel(@NonNull List<SessionDefinitionEntry> entries,
                                                       @NonNull String sessionName) {
        for (SessionDefinitionEntry entry : entries) {
            String projectSessionName = projectSessionPlanner.sessionNameForProjectLabel(entry.getGroupLabel());
            if (projectSessionName != null && projectSessionName.equals(sessionName)) {
                return entry.getGroupLabel();
            }
        }
        return null;
    }

    private static boolean isUrl(@NonNull String sessionName) {
        return sessionName.contains(URL_SCHEME_SEPARATOR);
    }

    private static boolean sameGroup(@Nullable String firstGroupLabel, @Nullable String secondGroupLabel) {
        if (firstGroupLabel == null) {
            return secondGroupLabel == null;
        }
        return firstGroupLabel.equals(secondGroupLabel);
    }
}
