package com.termux.app.sessiondefinition;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class DefaultProjectManagerSessionPlanner {

    public static final String PROJECT_MANAGER_SESSION_NAME_SUFFIX = "PM";

    @NonNull
    public List<String> planSessionNames(@NonNull List<SessionDefinitionEntry> entries) {
        List<String> sessionNames = new ArrayList<>();
        Set<String> seenProjectLabels = new LinkedHashSet<>();
        for (SessionDefinitionEntry entry : entries) {
            String sessionName = sessionNameForProjectLabel(entry.getGroupLabel());
            if (sessionName == null) {
                continue;
            }
            if (!seenProjectLabels.add(entry.getGroupLabel())) {
                continue;
            }
            sessionNames.add(sessionName);
        }
        return sessionNames;
    }

    @Nullable
    public String sessionNameForProjectLabel(@Nullable String projectLabel) {
        if (projectLabel == null) {
            return null;
        }
        String trimmedProjectLabel = projectLabel.trim();
        if (trimmedProjectLabel.isEmpty()) {
            return null;
        }
        return trimmedProjectLabel + PROJECT_MANAGER_SESSION_NAME_SUFFIX;
    }
}
