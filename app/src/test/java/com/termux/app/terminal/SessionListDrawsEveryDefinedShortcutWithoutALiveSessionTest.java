package com.termux.app.terminal;

import com.termux.app.sessiondefinition.DefaultProjectManagerSessionPlanner;
import com.termux.app.sessiondefinition.SessionDefinitionEntry;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The session list draws one shortcut row for every name the session definition carries, and it
 * decides that from the session definition alone. Whether a session is live, and whether the owner
 * pinned a name to the not-applicable group, are local state: they may move a row and they may hide
 * a row, and they may never decide that a row exists at all. While a pinned name only reached the
 * not-applicable group through a live session index, every pinned name the owner had not opened yet
 * drew no row anywhere, so its shortcut appeared only after the session had been opened once.
 */
public class SessionListDrawsEveryDefinedShortcutWithoutALiveSessionTest {

    private static final String NA = "N/A";
    private static final String FIRST_PROJECT_LABEL = "projectone";
    private static final String SECOND_PROJECT_LABEL = "projecttwo";
    private static final String FIRST_PROJECT_MANAGER_SESSION_NAME = FIRST_PROJECT_LABEL
        + DefaultProjectManagerSessionPlanner.PROJECT_MANAGER_SESSION_NAME_SUFFIX;
    private static final String SECOND_PROJECT_MANAGER_SESSION_NAME = SECOND_PROJECT_LABEL
        + DefaultProjectManagerSessionPlanner.PROJECT_MANAGER_SESSION_NAME_SUFFIX;
    private static final String FIRST_STORY_LABEL = "storyone";
    private static final String SECOND_STORY_LABEL = "storytwo";
    private static final String FIRST_STORY_SESSION_NAME = "https://example.test/story-one";
    private static final String SECOND_STORY_SESSION_NAME = "https://example.test/story-two";

    private final SessionHierarchyBuilder builder = new SessionHierarchyBuilder();

    private static SessionDefinitionEntry firstProjectEntry() {
        return new SessionDefinitionEntry(FIRST_PROJECT_LABEL, FIRST_STORY_LABEL,
            Collections.singletonList(FIRST_STORY_SESSION_NAME));
    }

    private static SessionDefinitionEntry secondProjectEntry() {
        return new SessionDefinitionEntry(SECOND_PROJECT_LABEL, SECOND_STORY_LABEL,
            Collections.singletonList(SECOND_STORY_SESSION_NAME));
    }

    private static List<SessionDefinitionEntry> definitionNamingBothProjects() {
        return Arrays.asList(firstProjectEntry(), secondProjectEntry());
    }

    private static String dump(List<SessionHierarchyRow> rows) {
        StringBuilder text = new StringBuilder();
        for (SessionHierarchyRow row : rows) {
            text.append(row.getType())
                .append(row.isHeader() ? "|" + row.getLabel()
                    : "|index=" + row.getSessionIndex() + "|name=" + row.getSessionName())
                .append('\n');
        }
        return text.toString();
    }

    private static int rowCountForSessionName(List<SessionHierarchyRow> rows, String sessionName) {
        int rowCount = 0;
        for (SessionHierarchyRow row : rows) {
            if (!row.isHeader() && sessionName.equals(row.getSessionName())) {
                rowCount++;
            }
        }
        return rowCount;
    }

    private static String projectLabelOfRowFor(List<SessionHierarchyRow> rows, String sessionName) {
        String currentProjectLabel = null;
        for (SessionHierarchyRow row : rows) {
            if (row.getType() == SessionHierarchyRow.Type.PROJECT_HEADER) {
                currentProjectLabel = row.getLabel();
            } else if (!row.isHeader() && sessionName.equals(row.getSessionName())) {
                return currentProjectLabel;
            }
        }
        return null;
    }

    private static List<Integer> sessionIndexesDrawnMoreThanOnce(List<SessionHierarchyRow> rows) {
        List<Integer> repeatedSessionIndexes = new ArrayList<>();
        Set<Integer> seenSessionIndexes = new LinkedHashSet<>();
        for (Integer sessionIndex : SessionHierarchyBuilder.visibleSessionIndexes(rows)) {
            if (!seenSessionIndexes.add(sessionIndex)) {
                repeatedSessionIndexes.add(sessionIndex);
            }
        }
        return repeatedSessionIndexes;
    }

    @Test
    public void aStorySessionNamePinnedToTheNotApplicableGroupDrawsItsRowWhileNoSessionIsLive() {
        Set<String> alwaysNaSessionNames = Collections.singleton(FIRST_STORY_SESSION_NAME);

        List<SessionHierarchyRow> rows = builder.build(Collections.emptyList(),
            definitionNamingBothProjects(), NA, alwaysNaSessionNames);

        Assert.assertEquals("a session name the session definition carries must draw exactly one"
                + " shortcut row while the owner has it pinned to the not-applicable group and no"
                + " session is live for it; the pin decides where the row sits, never whether the row"
                + " exists. Actual:\n" + dump(rows),
            1, rowCountForSessionName(rows, FIRST_STORY_SESSION_NAME));
        Assert.assertEquals("a pinned name must draw its row under the not-applicable header."
                + " Actual:\n" + dump(rows),
            NA, projectLabelOfRowFor(rows, FIRST_STORY_SESSION_NAME));
    }

    @Test
    public void aProjectManagerNamePinnedToTheNotApplicableGroupDrawsItsRowWhileNoSessionIsLive() {
        Set<String> alwaysNaSessionNames = Collections.singleton(FIRST_PROJECT_MANAGER_SESSION_NAME);

        List<SessionHierarchyRow> rows = builder.build(Collections.emptyList(),
            definitionNamingBothProjects(), NA, alwaysNaSessionNames);

        Assert.assertEquals("a project-manager name is derived from a project label the session"
                + " definition names, so it must draw exactly one shortcut row while the owner has it"
                + " pinned to the not-applicable group and no session is live for it. Actual:\n"
                + dump(rows),
            1, rowCountForSessionName(rows, FIRST_PROJECT_MANAGER_SESSION_NAME));
        Assert.assertEquals("a pinned project-manager name must draw its row under the not-applicable"
                + " header. Actual:\n" + dump(rows),
            NA, projectLabelOfRowFor(rows, FIRST_PROJECT_MANAGER_SESSION_NAME));
    }

    @Test
    public void everyNameTheDefinitionCarriesDrawsOneRowWhileNoSessionIsLive() {
        Set<String> alwaysNaSessionNames = new LinkedHashSet<>(Arrays.asList(
            FIRST_STORY_SESSION_NAME, SECOND_PROJECT_MANAGER_SESSION_NAME));

        List<SessionHierarchyRow> rows = builder.build(Collections.emptyList(),
            definitionNamingBothProjects(), NA, alwaysNaSessionNames);

        for (String definedSessionName : Arrays.asList(FIRST_PROJECT_MANAGER_SESSION_NAME,
                SECOND_PROJECT_MANAGER_SESSION_NAME, FIRST_STORY_SESSION_NAME,
                SECOND_STORY_SESSION_NAME)) {
            Assert.assertEquals("the session list is built from the session definition alone, so every"
                    + " name the definition carries must draw exactly one shortcut row while nothing is"
                    + " live. Missing or repeated name: " + definedSessionName + "\nActual:\n"
                    + dump(rows),
                1, rowCountForSessionName(rows, definedSessionName));
        }
    }

    @Test
    public void aPinnedNameWhoseSessionIsLiveStillDrawsExactlyOneRow() {
        List<String> liveSessionNames =
            Arrays.asList(FIRST_STORY_SESSION_NAME, FIRST_PROJECT_MANAGER_SESSION_NAME);
        Set<String> alwaysNaSessionNames = new LinkedHashSet<>(Arrays.asList(
            FIRST_STORY_SESSION_NAME, FIRST_PROJECT_MANAGER_SESSION_NAME));

        List<SessionHierarchyRow> rows = builder.build(liveSessionNames,
            definitionNamingBothProjects(), NA, alwaysNaSessionNames);

        Assert.assertEquals("a pinned name whose session is live must draw one row only, carrying the"
                + " live session index. Actual:\n" + dump(rows),
            1, rowCountForSessionName(rows, FIRST_STORY_SESSION_NAME));
        Assert.assertEquals("a pinned project-manager name whose session is live must draw one row"
                + " only. Actual:\n" + dump(rows),
            1, rowCountForSessionName(rows, FIRST_PROJECT_MANAGER_SESSION_NAME));
        Assert.assertEquals("no live session may be drawn twice; a repeated index makes next and"
                + " previous session navigation visit it twice. Actual:\n" + dump(rows),
            Collections.emptyList(), sessionIndexesDrawnMoreThanOnce(rows));
        Assert.assertEquals("every live session must be counted once. Actual:\n" + dump(rows),
            liveSessionNames.size(),
            SessionHierarchyBuilder.visibleSessionIndexes(rows).size());
    }
}
