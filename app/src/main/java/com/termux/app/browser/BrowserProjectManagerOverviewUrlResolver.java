package com.termux.app.browser;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.app.sessiondefinition.DefaultProjectManagerSessionPlanner;
import com.termux.app.sessiondefinition.SessionDefinitionEntry;

import java.util.List;

public final class BrowserProjectManagerOverviewUrlResolver {

    public interface SessionDefinitionEntriesSupplier {
        @NonNull
        List<SessionDefinitionEntry> getSessionDefinitionEntries();
    }

    private final SessionDefinitionEntriesSupplier entriesSupplier;

    private final DefaultProjectManagerSessionPlanner managerSessionPlanner =
        new DefaultProjectManagerSessionPlanner();

    public BrowserProjectManagerOverviewUrlResolver(@NonNull SessionDefinitionEntriesSupplier entriesSupplier) {
        this.entriesSupplier = entriesSupplier;
    }

    @Nullable
    public String resolveOverviewUrlForManagerSessionName(@Nullable String sessionName) {
        if (sessionName == null || sessionName.isEmpty()) {
            return null;
        }
        List<SessionDefinitionEntry> entries = entriesSupplier.getSessionDefinitionEntries();
        for (SessionDefinitionEntry entry : entries) {
            String managerSessionName =
                managerSessionPlanner.sessionNameForProjectLabel(entry.getGroupLabel());
            if (managerSessionName == null || !managerSessionName.equals(sessionName)) {
                continue;
            }
            String overviewUrl = overviewUrlForGroupLabel(entries, entry.getGroupLabel());
            if (overviewUrl != null && !overviewUrl.isEmpty()) {
                return overviewUrl;
            }
        }
        return null;
    }

    @Nullable
    private static String overviewUrlForGroupLabel(@NonNull List<SessionDefinitionEntry> entries,
                                                   @Nullable String groupLabel) {
        for (SessionDefinitionEntry entry : entries) {
            if (!sameGroup(groupLabel, entry.getGroupLabel())) {
                continue;
            }
            String overviewUrl = entry.getOverviewUrl();
            if (overviewUrl != null && !overviewUrl.isEmpty()) {
                return overviewUrl;
            }
        }
        return null;
    }

    private static boolean sameGroup(@Nullable String firstGroupLabel, @Nullable String secondGroupLabel) {
        if (firstGroupLabel == null) {
            return secondGroupLabel == null;
        }
        return firstGroupLabel.equals(secondGroupLabel);
    }
}
