package com.termux.app.terminal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.app.sessiondefinition.SessionDefinitionEntry;
import com.termux.app.sessiondefinition.SessionDefinitionEntryMatcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class SessionHierarchyBuilder {

    private static final String HTTP_SCHEME_PREFIX = "http://";
    private static final String HTTPS_SCHEME_PREFIX = "https://";

    private final SessionDefinitionEntryMatcher mMatcher = new SessionDefinitionEntryMatcher();

    @NonNull
    public List<SessionHierarchyRow> build(@NonNull List<String> sessionNames,
                                           @NonNull List<SessionDefinitionEntry> entries,
                                           @NonNull String naProjectLabel) {
        return build(sessionNames, entries, naProjectLabel, Collections.emptySet());
    }

    @NonNull
    public List<SessionHierarchyRow> build(@NonNull List<String> sessionNames,
                                           @NonNull List<SessionDefinitionEntry> entries,
                                           @NonNull String naProjectLabel,
                                           @NonNull Set<String> alwaysNaSessionNames) {
        if (entries.isEmpty()) {
            return flatten(sessionNames);
        }

        Map<String, Integer> sessionIndexByName = new LinkedHashMap<>();
        List<Integer> unmatchedSessionIndexes = new ArrayList<>();
        for (int sessionIndex = 0; sessionIndex < sessionNames.size(); sessionIndex++) {
            String sessionName = sessionNames.get(sessionIndex);
            if (isAlwaysNaSessionName(sessionName, alwaysNaSessionNames)
                    || (mMatcher.findEntryForSessionName(entries, sessionName) == null
                        && !isOrphanedProjectSessionName(sessionName))) {
                unmatchedSessionIndexes.add(sessionIndex);
                continue;
            }
            if (sessionName != null && !sessionIndexByName.containsKey(sessionName)) {
                sessionIndexByName.put(sessionName, sessionIndex);
            }
        }

        Map<String, String> overviewUrlByProject = new LinkedHashMap<>();
        Map<String, String> tdpmConsoleUrlByProject = new LinkedHashMap<>();
        Map<String, String> newIssueUrlByProject = new LinkedHashMap<>();
        for (SessionDefinitionEntry entry : entries) {
            String overviewUrl = entry.getOverviewUrl();
            if (overviewUrl != null && !overviewUrlByProject.containsKey(entry.getGroupLabel())) {
                overviewUrlByProject.put(entry.getGroupLabel(), overviewUrl);
            }
            String tdpmConsoleUrl = entry.getTdpmConsoleUrl();
            if (tdpmConsoleUrl != null && !tdpmConsoleUrlByProject.containsKey(entry.getGroupLabel())) {
                tdpmConsoleUrlByProject.put(entry.getGroupLabel(), tdpmConsoleUrl);
            }
            String newIssueUrl = entry.getNewIssueUrl();
            if (newIssueUrl != null && !newIssueUrlByProject.containsKey(entry.getGroupLabel())) {
                newIssueUrlByProject.put(entry.getGroupLabel(), newIssueUrl);
            }
        }

        Map<String, Map<String, List<Integer>>> sessionIndexesByProjectAndStory = new LinkedHashMap<>();
        Set<String> placedNames = new HashSet<>();
        for (SessionDefinitionEntry entry : entries) {
            for (String url : entry.getUrls()) {
                if (isAlwaysNaSessionName(url, alwaysNaSessionNames)) {
                    continue;
                }
                Integer sessionIndex = sessionIndexByName.get(url);
                if (sessionIndex == null) {
                    continue;
                }
                if (!placedNames.add(url)) {
                    continue;
                }
                Map<String, List<Integer>> storiesInProject =
                    sessionIndexesByProjectAndStory.computeIfAbsent(entry.getGroupLabel(), key -> new LinkedHashMap<>());
                List<Integer> storySessionIndexes =
                    storiesInProject.computeIfAbsent(entry.getEntryLabel(), key -> new ArrayList<>());
                storySessionIndexes.add(sessionIndex);
            }
        }

        for (Map.Entry<String, Integer> candidate : sessionIndexByName.entrySet()) {
            if (!placedNames.contains(candidate.getKey())) {
                unmatchedSessionIndexes.add(candidate.getValue());
            }
        }
        Collections.sort(unmatchedSessionIndexes);

        List<SessionHierarchyRow> rows = new ArrayList<>();
        if (!unmatchedSessionIndexes.isEmpty()) {
            rows.add(SessionHierarchyRow.projectHeader(naProjectLabel));
            for (int sessionIndex : unmatchedSessionIndexes) {
                rows.add(SessionHierarchyRow.session(sessionIndex));
            }
        }
        for (Map.Entry<String, Map<String, List<Integer>>> project : sessionIndexesByProjectAndStory.entrySet()) {
            rows.add(SessionHierarchyRow.projectHeader(project.getKey(),
                overviewUrlByProject.get(project.getKey()), tdpmConsoleUrlByProject.get(project.getKey()),
                newIssueUrlByProject.get(project.getKey())));
            for (Map.Entry<String, List<Integer>> story : project.getValue().entrySet()) {
                rows.add(SessionHierarchyRow.storyHeader(story.getKey()));
                for (int sessionIndex : story.getValue()) {
                    rows.add(SessionHierarchyRow.session(sessionIndex));
                }
            }
        }
        return rows;
    }

    public static int firstSessionIndex(@NonNull List<SessionHierarchyRow> rows) {
        for (SessionHierarchyRow row : rows) {
            if (!row.isHeader()) {
                return row.getSessionIndex();
            }
        }
        return -1;
    }

    public static int firstSessionIndexForProject(@NonNull List<SessionHierarchyRow> rows,
                                                  @NonNull String normalizedProjectLabel) {
        boolean withinProject = false;
        for (SessionHierarchyRow row : rows) {
            if (row.getType() == SessionHierarchyRow.Type.PROJECT_HEADER) {
                withinProject = matchesProjectLabel(row, normalizedProjectLabel);
                continue;
            }
            if (withinProject && !row.isHeader()) {
                return row.getSessionIndex();
            }
        }
        return -1;
    }

    @Nullable
    public static String projectActionUrl(@NonNull List<SessionHierarchyRow> rows,
                                          @NonNull String normalizedProjectLabel,
                                          @NonNull ProjectAction action) {
        for (SessionHierarchyRow row : rows) {
            if (row.getType() == SessionHierarchyRow.Type.PROJECT_HEADER
                    && matchesProjectLabel(row, normalizedProjectLabel)) {
                return actionUrl(row, action);
            }
        }
        return null;
    }

    private static boolean matchesProjectLabel(@NonNull SessionHierarchyRow projectHeaderRow,
                                               @NonNull String normalizedProjectLabel) {
        String label = projectHeaderRow.getLabel();
        return label != null
            && ExpandedProjectsAllowlistParser.normalize(label).equals(normalizedProjectLabel);
    }

    @Nullable
    private static String actionUrl(@NonNull SessionHierarchyRow projectHeaderRow,
                                    @NonNull ProjectAction action) {
        switch (action) {
            case OVERVIEW_URL:
                return projectHeaderRow.getOverviewUrl();
            case TDPM_CONSOLE_URL:
                return projectHeaderRow.getTdpmConsoleUrl();
            case NEW_ISSUE_URL:
                return projectHeaderRow.getNewIssueUrl();
            default:
                return null;
        }
    }

    public static int rowPositionForSessionIndex(@NonNull List<SessionHierarchyRow> rows, int sessionIndex) {
        for (int position = 0; position < rows.size(); position++) {
            SessionHierarchyRow row = rows.get(position);
            if (!row.isHeader() && row.getSessionIndex() == sessionIndex) {
                return position;
            }
        }
        return -1;
    }

    @NonNull
    public static List<Integer> visibleSessionIndexes(@NonNull List<SessionHierarchyRow> visibleRows) {
        List<Integer> sessionIndexes = new ArrayList<>();
        for (SessionHierarchyRow row : visibleRows) {
            if (!row.isHeader()) {
                sessionIndexes.add(row.getSessionIndex());
            }
        }
        return sessionIndexes;
    }

    public static int nextVisibleSessionIndex(@NonNull List<Integer> visibleSessionIndexes,
                                              int currentSessionIndex, boolean forward) {
        if (visibleSessionIndexes.isEmpty()) {
            return -1;
        }
        int currentPosition = visibleSessionIndexes.indexOf(currentSessionIndex);
        if (currentPosition >= 0) {
            int size = visibleSessionIndexes.size();
            int nextPosition = forward
                ? (currentPosition + 1) % size
                : (currentPosition - 1 + size) % size;
            return visibleSessionIndexes.get(nextPosition);
        }
        return nearestVisibleSessionIndex(visibleSessionIndexes, currentSessionIndex, forward);
    }

    private static int nearestVisibleSessionIndex(@NonNull List<Integer> visibleSessionIndexes,
                                                  int currentSessionIndex, boolean forward) {
        Integer wrapCandidate = null;
        Integer directionalCandidate = null;
        for (int sessionIndex : visibleSessionIndexes) {
            if (forward) {
                if (sessionIndex > currentSessionIndex
                        && (directionalCandidate == null || sessionIndex < directionalCandidate)) {
                    directionalCandidate = sessionIndex;
                }
                if (wrapCandidate == null || sessionIndex < wrapCandidate) {
                    wrapCandidate = sessionIndex;
                }
            } else {
                if (sessionIndex < currentSessionIndex
                        && (directionalCandidate == null || sessionIndex > directionalCandidate)) {
                    directionalCandidate = sessionIndex;
                }
                if (wrapCandidate == null || sessionIndex > wrapCandidate) {
                    wrapCandidate = sessionIndex;
                }
            }
        }
        return directionalCandidate != null ? directionalCandidate : wrapCandidate;
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

    private static boolean isAlwaysNaSessionName(@Nullable String sessionName,
                                                 @NonNull Set<String> alwaysNaSessionNames) {
        return sessionName != null && alwaysNaSessionNames.contains(sessionName);
    }

    private static boolean isOrphanedProjectSessionName(@Nullable String sessionName) {
        if (sessionName == null) {
            return false;
        }
        String normalizedSessionName = sessionName.trim().toLowerCase(Locale.ROOT);
        return normalizedSessionName.startsWith(HTTP_SCHEME_PREFIX)
            || normalizedSessionName.startsWith(HTTPS_SCHEME_PREFIX);
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
