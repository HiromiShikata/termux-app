package com.termux.app.sessiondefinition;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SessionDefinitionLoadResult {

    private final List<SessionDefinitionEntry> entries;
    private final int totalGroupCount;
    private final List<String> failedGroupLabels;

    public SessionDefinitionLoadResult(@NonNull List<SessionDefinitionEntry> entries, int totalGroupCount,
                                       @NonNull List<String> failedGroupLabels) {
        this.entries = Collections.unmodifiableList(new ArrayList<>(entries));
        this.totalGroupCount = totalGroupCount;
        this.failedGroupLabels = Collections.unmodifiableList(new ArrayList<>(failedGroupLabels));
    }

    @NonNull
    public List<SessionDefinitionEntry> getEntries() {
        return entries;
    }

    public int getTotalGroupCount() {
        return totalGroupCount;
    }

    @NonNull
    public List<String> getFailedGroupLabels() {
        return failedGroupLabels;
    }

    public int getFailedGroupCount() {
        return failedGroupLabels.size();
    }

    public boolean hasFailedGroups() {
        return !failedGroupLabels.isEmpty();
    }

    public boolean isAuthoritative() {
        return !entries.isEmpty() && failedGroupLabels.isEmpty();
    }
}
