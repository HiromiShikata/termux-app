package com.termux.app.terminal;

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
 * Locks the two session list properties that a released build violated: the project manager row of a
 * project is the first row inside that project's own group, and no row is drawn for a session name
 * that is neither a live session nor a name the session definition carries. The violation drew one
 * row per accumulated stored name, placed every one of those rows under the not-applicable header,
 * and left each of them with no live session to navigate to.
 */
public class SessionListDrawsNoRowForAnUnknownNameTest {

    private static final String NA = "N/A";
    private static final String PROJECT_LABEL = "demoproject";
    private static final String PROJECT_MANAGER_SESSION_NAME = "demoprojectpm";
    private static final String STORY_LABEL = "demostory";
    private static final String STORY_SESSION_NAME = "https://example.test/story-session";

    private final SessionHierarchyBuilder builder = new SessionHierarchyBuilder();

    private static List<SessionDefinitionEntry> definitionWithAProjectManagerAndOneStorySession() {
        return Collections.singletonList(new SessionDefinitionEntry(PROJECT_LABEL, STORY_LABEL,
            Arrays.asList(PROJECT_MANAGER_SESSION_NAME, STORY_SESSION_NAME)));
    }

    private static Set<String> namesTheDefinitionCarries() {
        return new LinkedHashSet<>(Arrays.asList(PROJECT_MANAGER_SESSION_NAME, STORY_SESSION_NAME));
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

    private static int indexOfProjectHeader(List<SessionHierarchyRow> rows, String label) {
        for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
            SessionHierarchyRow row = rows.get(rowIndex);
            if (row.getType() == SessionHierarchyRow.Type.PROJECT_HEADER
                    && label.equals(row.getLabel())) {
                return rowIndex;
            }
        }
        return -1;
    }

    @Test
    public void theProjectManagerRowOfALiveProjectManagerSessionIsTheFirstRowInsideItsProjectGroup() {
        List<String> liveSessionNames = Arrays.asList(STORY_SESSION_NAME, PROJECT_MANAGER_SESSION_NAME);

        List<SessionHierarchyRow> rows =
            builder.build(liveSessionNames, definitionWithAProjectManagerAndOneStorySession(), NA);

        int projectHeaderIndex = indexOfProjectHeader(rows, PROJECT_LABEL);
        Assert.assertTrue("the project group must have a header. Actual:\n" + dump(rows),
            projectHeaderIndex >= 0);
        SessionHierarchyRow firstRowInsideTheProject = rows.get(projectHeaderIndex + 1);
        Assert.assertFalse("the first row inside the project group must be a session row, not a header."
            + " Actual:\n" + dump(rows), firstRowInsideTheProject.isHeader());
        Assert.assertEquals("the project manager session must be the first row inside its project group."
                + " Actual:\n" + dump(rows),
            PROJECT_MANAGER_SESSION_NAME, firstRowInsideTheProject.getSessionName());
        Assert.assertEquals("the project manager row must carry the live session index."
                + " Actual:\n" + dump(rows),
            liveSessionNames.indexOf(PROJECT_MANAGER_SESSION_NAME),
            firstRowInsideTheProject.getSessionIndex());
        Assert.assertEquals("the project manager session must not be placed under the not-applicable"
            + " header. Actual:\n" + dump(rows), -1, indexOfProjectHeader(rows, NA));
    }

    @Test
    public void theProjectManagerRowStaysFirstInsideItsProjectGroupWhileItHasNoLiveSession() {
        List<SessionHierarchyRow> rows = builder.build(Collections.singletonList(STORY_SESSION_NAME),
            definitionWithAProjectManagerAndOneStorySession(), NA);

        int projectHeaderIndex = indexOfProjectHeader(rows, PROJECT_LABEL);
        Assert.assertTrue("the project group must have a header. Actual:\n" + dump(rows),
            projectHeaderIndex >= 0);
        SessionHierarchyRow firstRowInsideTheProject = rows.get(projectHeaderIndex + 1);
        Assert.assertEquals("the project manager session must be the first row inside its project group"
                + " even while it has no live session. Actual:\n" + dump(rows),
            PROJECT_MANAGER_SESSION_NAME, firstRowInsideTheProject.getSessionName());
        Assert.assertEquals("the project manager session must not be placed under the not-applicable"
            + " header. Actual:\n" + dump(rows), -1, indexOfProjectHeader(rows, NA));
    }

    @Test
    public void everyDrawnSessionRowNamesEitherALiveSessionOrANameTheDefinitionCarries() {
        List<String> liveSessionNames = Arrays.asList(STORY_SESSION_NAME, PROJECT_MANAGER_SESSION_NAME,
            "https://example.test/live-session-outside-the-definition");
        Set<String> definedNames = namesTheDefinitionCarries();

        List<SessionHierarchyRow> rows =
            builder.build(liveSessionNames, definitionWithAProjectManagerAndOneStorySession(), NA);

        List<String> rowsNamingNeitherALiveSessionNorADefinedName = new ArrayList<>();
        for (SessionHierarchyRow row : rows) {
            if (row.isHeader()) continue;
            int sessionIndex = row.getSessionIndex();
            if (sessionIndex >= 0 && sessionIndex < liveSessionNames.size()) continue;
            if (row.getSessionName() != null && definedNames.contains(row.getSessionName())) continue;
            rowsNamingNeitherALiveSessionNorADefinedName.add(
                "index=" + sessionIndex + " name=" + row.getSessionName());
        }
        Assert.assertEquals("no row may be drawn for a session name that is neither a live session nor a"
                + " name the session definition carries. Offending rows: "
                + rowsNamingNeitherALiveSessionNorADefinedName + "\nActual:\n" + dump(rows),
            Collections.emptyList(), rowsNamingNeitherALiveSessionNorADefinedName);
    }

    @Test
    public void aLiveSessionOutsideTheDefinitionKeepsExactlyOneRowUnderTheNotApplicableHeader() {
        String liveSessionOutsideTheDefinition = "https://example.test/live-session-outside-the-definition";
        List<String> liveSessionNames = Arrays.asList(STORY_SESSION_NAME, PROJECT_MANAGER_SESSION_NAME,
            liveSessionOutsideTheDefinition);

        List<SessionHierarchyRow> rows =
            builder.build(liveSessionNames, definitionWithAProjectManagerAndOneStorySession(), NA);

        int rowCountForThatSession = 0;
        for (SessionHierarchyRow row : rows) {
            if (!row.isHeader()
                    && row.getSessionIndex() == liveSessionNames.indexOf(liveSessionOutsideTheDefinition)) {
                rowCountForThatSession++;
            }
        }
        Assert.assertEquals("a live session the definition does not carry keeps exactly one row."
            + " Actual:\n" + dump(rows), 1, rowCountForThatSession);
        Assert.assertTrue("that session must be placed under the not-applicable header. Actual:\n"
            + dump(rows), indexOfProjectHeader(rows, NA) >= 0);
    }
}
