package com.termux.app.terminal;

import androidx.annotation.NonNull;

import com.termux.app.sessiondefinition.SessionDefinitionEntry;
import com.termux.app.sessiondefinition.SessionDefinitionEntryMatcher;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class SessionHierarchyBuilder {

    private final SessionDefinitionEntryMatcher mMatcher = new SessionDefinitionEntryMatcher();

    @NonNull
    public List<SessionHierarchyRow> build(@NonNull List<String> sessionNames,
                                           @NonNull List<SessionDefinitionEntry> entries,
                                           @NonNull String otherProjectLabel) {
        if (entries.isEmpty()) {
            return flatten(sessionNames);
        }

        Map<String, Map<String, List<Integer>>> sessionIndexesByProjectAndStory = new LinkedHashMap<>();
        List<Integer> unmatchedSessionIndexes = new ArrayList<>();

        for (int sessionIndex = 0; sessionIndex < sessionNames.size(); sessionIndex++) {
            SessionDefinitionEntry entry = mMatcher.findEntryForSessionName(entries, sessionNames.get(sessionIndex));
            if (entry == null) {
                unmatchedSessionIndexes.add(sessionIndex);
                continue;
            }
            Map<String, List<Integer>> storiesInProject =
                sessionIndexesByProjectAndStory.computeIfAbsent(entry.getGroupLabel(), key -> new LinkedHashMap<>());
            List<Integer> storySessionIndexes =
                storiesInProject.computeIfAbsent(entry.getEntryLabel(), key -> new ArrayList<>());
            storySessionIndexes.add(sessionIndex);
        }

        List<SessionHierarchyRow> rows = new ArrayList<>();
        for (Map.Entry<String, Map<String, List<Integer>>> project : sessionIndexesByProjectAndStory.entrySet()) {
            rows.add(SessionHierarchyRow.projectHeader(project.getKey()));
            for (Map.Entry<String, List<Integer>> story : project.getValue().entrySet()) {
                rows.add(SessionHierarchyRow.storyHeader(story.getKey()));
                for (int sessionIndex : story.getValue()) {
                    rows.add(SessionHierarchyRow.session(sessionIndex));
                }
            }
        }
        if (!unmatchedSessionIndexes.isEmpty()) {
            rows.add(SessionHierarchyRow.projectHeader(otherProjectLabel));
            for (int sessionIndex : unmatchedSessionIndexes) {
                rows.add(SessionHierarchyRow.session(sessionIndex));
            }
        }
        return rows;
    }

    @NonNull
    public List<SessionHierarchyRow> filterCollapsedProjects(@NonNull List<SessionHierarchyRow> rows,
                                                             @NonNull Set<String> collapsedProjectKeys) {
        if (collapsedProjectKeys.isEmpty()) {
            return rows;
        }
        List<SessionHierarchyRow> visibleRows = new ArrayList<>(rows.size());
        boolean currentProjectCollapsed = false;
        for (SessionHierarchyRow row : rows) {
            if (row.getType() == SessionHierarchyRow.Type.PROJECT_HEADER) {
                currentProjectCollapsed = collapsedProjectKeys.contains(row.getLabel());
                visibleRows.add(row);
            } else if (!currentProjectCollapsed) {
                visibleRows.add(row);
            }
        }
        return visibleRows;
    }

    @NonNull
    private List<SessionHierarchyRow> flatten(@NonNull List<String> sessionNames) {
        List<SessionHierarchyRow> rows = new ArrayList<>();
        for (int sessionIndex = 0; sessionIndex < sessionNames.size(); sessionIndex++) {
            rows.add(SessionHierarchyRow.session(sessionIndex));
        }
        return rows;
    }
}
