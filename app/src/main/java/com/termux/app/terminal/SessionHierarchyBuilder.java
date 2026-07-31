package com.termux.app.terminal;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.app.sessiondefinition.DefaultProjectManagerSessionPlanner;
import com.termux.app.sessiondefinition.SessionDefinitionEntry;
import com.termux.app.sessiondefinition.SessionDefinitionEntryMatcher;
import com.termux.shared.termux.settings.preferences.HiddenSessionNameMatcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class SessionHierarchyBuilder {

    private static final String HTTP_SCHEME_PREFIX = "http://";
    private static final String HTTPS_SCHEME_PREFIX = "https://";

    private final SessionDefinitionEntryMatcher mMatcher = new SessionDefinitionEntryMatcher();
    private final DefaultProjectManagerSessionPlanner mDefaultProjectManagerSessionPlanner =
        new DefaultProjectManagerSessionPlanner();

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
        return build(sessionNames, entries, naProjectLabel, alwaysNaSessionNames,
            Collections.emptySet());
    }

    @NonNull
    public List<SessionHierarchyRow> build(@NonNull List<String> sessionNames,
                                           @NonNull List<SessionDefinitionEntry> entries,
                                           @NonNull String naProjectLabel,
                                           @NonNull Set<String> alwaysNaSessionNames,
                                           @NonNull Set<String> deletedSessionNames) {
        if (entries.isEmpty()) {
            return flatten(sessionNames);
        }

        Map<String, String> projectLabelByManagerSessionName = projectLabelByManagerSessionName(entries);
        Set<String> notApplicableDefinitionNames = new LinkedHashSet<>();
        for (String managerSessionName : projectLabelByManagerSessionName.keySet()) {
            if (isAlwaysNaSessionName(managerSessionName, alwaysNaSessionNames)) {
                notApplicableDefinitionNames.add(managerSessionName);
            }
        }

        Map<String, Integer> sessionIndexByName = new LinkedHashMap<>();
        Map<String, Integer> liveSessionIndexByName = new LinkedHashMap<>();
        Map<String, Integer> managerSessionIndexByProjectLabel = new LinkedHashMap<>();
        List<Integer> unmatchedSessionIndexes = new ArrayList<>();
        for (int sessionIndex = 0; sessionIndex < sessionNames.size(); sessionIndex++) {
            String sessionName = sessionNames.get(sessionIndex);
            if (sessionName != null && !liveSessionIndexByName.containsKey(sessionName)) {
                liveSessionIndexByName.put(sessionName, sessionIndex);
            }
            if (!isAlwaysNaSessionName(sessionName, alwaysNaSessionNames)
                    && projectLabelByManagerSessionName.containsKey(sessionName)) {
                String projectLabel = projectLabelByManagerSessionName.get(sessionName);
                if (!managerSessionIndexByProjectLabel.containsKey(projectLabel)) {
                    managerSessionIndexByProjectLabel.put(projectLabel, sessionIndex);
                    continue;
                }
            }
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

        Map<String, Map<String, List<String>>> sessionNamesByProjectAndStory = new LinkedHashMap<>();
        Set<String> placedNames = new HashSet<>();
        for (SessionDefinitionEntry entry : entries) {
            for (String url : entry.getUrls()) {
                if (url == null || url.isEmpty()) {
                    continue;
                }
                if (isAlwaysNaSessionName(url, alwaysNaSessionNames)) {
                    placedNames.add(url);
                    notApplicableDefinitionNames.add(url);
                    continue;
                }
                if (projectLabelByManagerSessionName.containsKey(url)) {
                    placedNames.add(url);
                    continue;
                }
                if (!placedNames.add(url)) {
                    continue;
                }
                Map<String, List<String>> storiesInProject =
                    sessionNamesByProjectAndStory.computeIfAbsent(entry.getGroupLabel(), key -> new LinkedHashMap<>());
                List<String> storySessionNames =
                    storiesInProject.computeIfAbsent(entry.getEntryLabel(), key -> new ArrayList<>());
                storySessionNames.add(url);
            }
        }

        for (Map.Entry<String, Integer> candidate : sessionIndexByName.entrySet()) {
            if (!placedNames.contains(candidate.getKey())) {
                unmatchedSessionIndexes.add(candidate.getValue());
            }
        }
        Collections.sort(unmatchedSessionIndexes);

        List<SessionHierarchyRow> rows = new ArrayList<>();
        List<SessionHierarchyRow> notApplicableRows = new ArrayList<>();
        for (int sessionIndex : unmatchedSessionIndexes) {
            notApplicableRows.add(sessionRow(sessionNames, sessionIndex));
        }
        for (String notApplicableDefinitionName : notApplicableDefinitionNames) {
            if (!liveSessionIndexByName.containsKey(notApplicableDefinitionName)) {
                notApplicableRows.add(
                    definitionBackedSessionRow(liveSessionIndexByName, notApplicableDefinitionName));
            }
        }
        if (!notApplicableRows.isEmpty()) {
            rows.add(SessionHierarchyRow.projectHeader(naProjectLabel));
            rows.addAll(notApplicableRows);
        }
        Set<String> definedProjectLabels = new LinkedHashSet<>();
        for (SessionDefinitionEntry entry : entries) {
            definedProjectLabels.add(entry.getGroupLabel());
        }
        for (String projectLabel : definedProjectLabels) {
            rows.add(SessionHierarchyRow.projectHeader(projectLabel,
                overviewUrlByProject.get(projectLabel), tdpmConsoleUrlByProject.get(projectLabel),
                newIssueUrlByProject.get(projectLabel)));
            String managerSessionName = drawableManagerSessionNameOwnedByProject(projectLabel,
                projectLabelByManagerSessionName, alwaysNaSessionNames);
            if (managerSessionName != null) {
                Integer managerSessionIndex = managerSessionIndexByProjectLabel.get(projectLabel);
                rows.add(managerSessionIndex == null
                    ? definitionBackedSessionRow(liveSessionIndexByName, managerSessionName)
                    : sessionRow(sessionNames, managerSessionIndex));
            }
            Map<String, List<String>> storiesInProject = sessionNamesByProjectAndStory.get(projectLabel);
            if (storiesInProject == null) {
                continue;
            }
            for (Map.Entry<String, List<String>> story : storiesInProject.entrySet()) {
                rows.add(SessionHierarchyRow.storyHeader(story.getKey()));
                for (String storySessionName : story.getValue()) {
                    rows.add(definitionBackedSessionRow(liveSessionIndexByName, storySessionName));
                }
            }
        }
        return rows;
    }

    public static final int NO_LIVE_SESSION_INDEX = -1;

    /**
     * A project-manager session name is derived from a project label, so the session definition
     * entries do not have to carry it, and a project can be named by the definition while no live
     * session exists for its project-manager name. Such a project still owns a project-manager row,
     * drawn from the derived name the same way a story row is drawn from a name the definition lists.
     * The derived name drops the surrounding whitespace of the project label, so two labels that
     * differ only by whitespace derive one name and only the label that owns that name draws its row.
     * A name the owner pinned to the not-applicable group keeps its single row there instead.
     */
    @Nullable
    private String drawableManagerSessionNameOwnedByProject(
            @NonNull String projectLabel,
            @NonNull Map<String, String> projectLabelByManagerSessionName,
            @NonNull Set<String> alwaysNaSessionNames) {
        String managerSessionName =
            mDefaultProjectManagerSessionPlanner.sessionNameForProjectLabel(projectLabel);
        if (managerSessionName == null
                || !projectLabel.equals(projectLabelByManagerSessionName.get(managerSessionName))) {
            return null;
        }
        return isAlwaysNaSessionName(managerSessionName, alwaysNaSessionNames)
            ? null : managerSessionName;
    }

    @NonNull
    private static SessionHierarchyRow definitionBackedSessionRow(
            @NonNull Map<String, Integer> liveSessionIndexByName, @NonNull String sessionName) {
        Integer liveSessionIndex = liveSessionIndexByName.get(sessionName);
        return SessionHierarchyRow.session(
            liveSessionIndex == null ? NO_LIVE_SESSION_INDEX : liveSessionIndex, sessionName);
    }

    public static int firstSessionIndex(@NonNull List<SessionHierarchyRow> rows) {
        for (SessionHierarchyRow row : rows) {
            if (!row.isHeader() && row.getSessionIndex() != NO_LIVE_SESSION_INDEX) {
                return row.getSessionIndex();
            }
        }
        return NO_LIVE_SESSION_INDEX;
    }

    public static int firstSessionIndexForProject(@NonNull List<SessionHierarchyRow> rows,
                                                  @NonNull String normalizedProjectLabel) {
        boolean withinProject = false;
        for (SessionHierarchyRow row : rows) {
            if (row.getType() == SessionHierarchyRow.Type.PROJECT_HEADER) {
                withinProject = matchesProjectLabel(row, normalizedProjectLabel);
                continue;
            }
            if (withinProject && !row.isHeader() && row.getSessionIndex() != NO_LIVE_SESSION_INDEX) {
                return row.getSessionIndex();
            }
        }
        return NO_LIVE_SESSION_INDEX;
    }

    @Nullable
    public static String projectActionUrl(@NonNull List<SessionHierarchyRow> rows,
                                          @NonNull String normalizedProjectLabel,
                                          @NonNull ProjectAction action) {
        SessionHierarchyRow projectHeaderRow = projectHeaderRowForProject(rows, normalizedProjectLabel);
        return projectHeaderRow == null ? null : actionUrl(projectHeaderRow, action);
    }

    @Nullable
    public static SessionHierarchyRow projectHeaderRowForProject(@NonNull List<SessionHierarchyRow> rows,
                                                                 @NonNull String normalizedProjectLabel) {
        for (SessionHierarchyRow row : rows) {
            if (row.getType() == SessionHierarchyRow.Type.PROJECT_HEADER
                    && matchesProjectLabel(row, normalizedProjectLabel)) {
                return row;
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

    public static int totalSessionCount(@NonNull List<SessionHierarchyRow> rows) {
        int sessionCount = 0;
        for (SessionHierarchyRow row : rows) {
            if (!row.isHeader()) {
                sessionCount++;
            }
        }
        return sessionCount;
    }

    @NonNull
    public static Map<String, Integer> sessionCountByProjectLabel(@NonNull List<SessionHierarchyRow> rows) {
        Map<String, Integer> sessionCountByProjectLabel = new LinkedHashMap<>();
        String currentProjectLabel = null;
        for (SessionHierarchyRow row : rows) {
            if (row.getType() == SessionHierarchyRow.Type.PROJECT_HEADER) {
                currentProjectLabel = row.getLabel();
                sessionCountByProjectLabel.putIfAbsent(currentProjectLabel, 0);
            } else if (!row.isHeader() && currentProjectLabel != null) {
                sessionCountByProjectLabel.merge(currentProjectLabel, 1, Integer::sum);
            }
        }
        return sessionCountByProjectLabel;
    }

    public static int pendingCallSessionCount(@NonNull List<SessionHierarchyRow> rows,
                                              @NonNull List<String> sessionNamesByIndex,
                                              @NonNull Set<String> pendingCallSessionNames) {
        int pendingCallSessionCount = 0;
        for (SessionHierarchyRow row : rows) {
            if (!row.isHeader() && isPendingCallSession(row, sessionNamesByIndex, pendingCallSessionNames)) {
                pendingCallSessionCount++;
            }
        }
        return pendingCallSessionCount;
    }

    @NonNull
    public static Map<String, Integer> pendingCallSessionCountByProjectLabel(
            @NonNull List<SessionHierarchyRow> rows,
            @NonNull List<String> sessionNamesByIndex,
            @NonNull Set<String> pendingCallSessionNames) {
        Map<String, Integer> pendingCallSessionCountByProjectLabel = new LinkedHashMap<>();
        String currentProjectLabel = null;
        for (SessionHierarchyRow row : rows) {
            if (row.getType() == SessionHierarchyRow.Type.PROJECT_HEADER) {
                currentProjectLabel = row.getLabel();
                pendingCallSessionCountByProjectLabel.putIfAbsent(currentProjectLabel, 0);
            } else if (!row.isHeader() && currentProjectLabel != null
                    && isPendingCallSession(row, sessionNamesByIndex, pendingCallSessionNames)) {
                pendingCallSessionCountByProjectLabel.merge(currentProjectLabel, 1, Integer::sum);
            }
        }
        return pendingCallSessionCountByProjectLabel;
    }

    private static boolean isPendingCallSession(@NonNull SessionHierarchyRow sessionRow,
                                                @NonNull List<String> sessionNamesByIndex,
                                                @NonNull Set<String> pendingCallSessionNames) {
        String sessionName = resolveRowSessionName(sessionRow, sessionNamesByIndex);
        return sessionName != null && pendingCallSessionNames.contains(sessionName);
    }

    @NonNull
    public static List<Integer> visibleSessionIndexes(@NonNull List<SessionHierarchyRow> visibleRows) {
        List<Integer> sessionIndexes = new ArrayList<>();
        for (SessionHierarchyRow row : visibleRows) {
            if (!row.isHeader() && row.getSessionIndex() != NO_LIVE_SESSION_INDEX) {
                sessionIndexes.add(row.getSessionIndex());
            }
        }
        return sessionIndexes;
    }

    @NonNull
    public static List<SessionHierarchyRow> filterHiddenSessions(@NonNull List<SessionHierarchyRow> rows,
                                                                 @NonNull List<String> sessionNamesByIndex,
                                                                 @NonNull Set<String> hiddenSessionNames) {
        if (hiddenSessionNames.isEmpty()) {
            return rows;
        }
        List<SessionHierarchyRow> retainedSessionRows = new ArrayList<>(rows.size());
        for (SessionHierarchyRow row : rows) {
            if (row.isHeader() || !isHiddenSession(row, sessionNamesByIndex, hiddenSessionNames)) {
                retainedSessionRows.add(row);
            }
        }
        return dropHeadersWithoutSessions(retainedSessionRows);
    }

    @NonNull
    private static List<SessionHierarchyRow> dropHeadersWithoutSessions(@NonNull List<SessionHierarchyRow> rows) {
        List<SessionHierarchyRow> visibleRows = new ArrayList<>(rows.size());
        for (int position = 0; position < rows.size(); position++) {
            SessionHierarchyRow row = rows.get(position);
            if (row.isHeader() && !headerPrecedesSession(rows, position)) {
                continue;
            }
            visibleRows.add(row);
        }
        return visibleRows;
    }

    private static boolean headerPrecedesSession(@NonNull List<SessionHierarchyRow> rows, int headerPosition) {
        SessionHierarchyRow.Type headerType = rows.get(headerPosition).getType();
        for (int position = headerPosition + 1; position < rows.size(); position++) {
            SessionHierarchyRow row = rows.get(position);
            if (!row.isHeader()) {
                return true;
            }
            if (headerType == SessionHierarchyRow.Type.PROJECT_HEADER
                    && row.getType() == SessionHierarchyRow.Type.PROJECT_HEADER) {
                return false;
            }
            if (headerType == SessionHierarchyRow.Type.STORY_HEADER) {
                return false;
            }
        }
        return false;
    }

    public static int shownSessionCount(@NonNull List<SessionHierarchyRow> rows,
                                        @NonNull List<String> sessionNamesByIndex,
                                        @NonNull Set<String> hiddenSessionNames) {
        int shownSessionCount = 0;
        for (SessionHierarchyRow row : rows) {
            if (!row.isHeader() && !isHiddenSession(row, sessionNamesByIndex, hiddenSessionNames)) {
                shownSessionCount++;
            }
        }
        return shownSessionCount;
    }

    private static boolean isHiddenSession(@NonNull SessionHierarchyRow sessionRow,
                                           @NonNull List<String> sessionNamesByIndex,
                                           @NonNull Set<String> hiddenSessionNames) {
        String sessionName = resolveRowSessionName(sessionRow, sessionNamesByIndex);
        return HiddenSessionNameMatcher.matchesAHiddenSession(sessionName, hiddenSessionNames);
    }

    @Nullable
    private static String resolveRowSessionName(@NonNull SessionHierarchyRow sessionRow,
                                                @NonNull List<String> sessionNamesByIndex) {
        String rowSessionName = sessionRow.getSessionName();
        if (rowSessionName != null) {
            return rowSessionName;
        }
        int sessionIndex = sessionRow.getSessionIndex();
        return sessionIndex >= 0 && sessionIndex < sessionNamesByIndex.size()
            ? sessionNamesByIndex.get(sessionIndex) : null;
    }

    @Nullable
    public static String projectLabelForSessionName(@NonNull List<SessionHierarchyRow> rows,
                                                    @Nullable String sessionName) {
        if (sessionName == null || sessionName.isEmpty()) {
            return null;
        }
        String currentProjectLabel = null;
        for (SessionHierarchyRow row : rows) {
            if (row.getType() == SessionHierarchyRow.Type.PROJECT_HEADER) {
                currentProjectLabel = row.getLabel();
            } else if (row.getType() == SessionHierarchyRow.Type.SESSION
                && sessionName.equals(row.getSessionName())) {
                return currentProjectLabel;
            }
        }
        return null;
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
    public static List<SessionHierarchyRow> filterCollapsedProjectSessions(
            @NonNull List<SessionHierarchyRow> rows,
            @NonNull Set<String> collapsedProjectKeys) {
        if (collapsedProjectKeys.isEmpty()) {
            return rows;
        }
        List<SessionHierarchyRow> countedRows = new ArrayList<>(rows.size());
        boolean currentProjectCollapsed = false;
        for (SessionHierarchyRow row : rows) {
            if (row.getType() == SessionHierarchyRow.Type.PROJECT_HEADER) {
                currentProjectCollapsed = collapsedProjectKeys.contains(row.getLabel());
                countedRows.add(row);
            } else if (!currentProjectCollapsed) {
                countedRows.add(row);
            }
        }
        return countedRows;
    }

    @NonNull
    private Map<String, String> projectLabelByManagerSessionName(@NonNull List<SessionDefinitionEntry> entries) {
        Map<String, String> projectLabelByManagerSessionName = new LinkedHashMap<>();
        for (SessionDefinitionEntry entry : entries) {
            String managerSessionName =
                mDefaultProjectManagerSessionPlanner.sessionNameForProjectLabel(entry.getGroupLabel());
            if (managerSessionName == null) {
                continue;
            }
            projectLabelByManagerSessionName.putIfAbsent(managerSessionName, entry.getGroupLabel());
        }
        return projectLabelByManagerSessionName;
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
            rows.add(sessionRow(sessionNames, sessionIndex));
        }
        return rows;
    }

    @NonNull
    private static SessionHierarchyRow sessionRow(@NonNull List<String> sessionNames, int sessionIndex) {
        String sessionName = sessionIndex >= 0 && sessionIndex < sessionNames.size()
            ? sessionNames.get(sessionIndex) : null;
        return SessionHierarchyRow.session(sessionIndex, sessionName);
    }
}
