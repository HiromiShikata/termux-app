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
 * Locks the session list property that a released build violated: no row is drawn for a session name
 * that is neither a live session nor a name the session definition carries. The violation drew one
 * row per accumulated stored name, placed every one of those rows under the not-applicable header,
 * and left each of them with no live session to navigate to.
 */
public class SessionListDrawsNoRowForAnUnknownNameTest {

    private static final String NA = "N/A";
    private static final String PROJECT_LABEL = "demoproject";
    private static final String STORY_LABEL = "demostory";
    private static final String STORY_SESSION_NAME = "https://example.test/story-session";

    private final SessionHierarchyBuilder builder = new SessionHierarchyBuilder();

    private static List<SessionDefinitionEntry> definitionWithOneStorySession() {
        return Collections.singletonList(new SessionDefinitionEntry(PROJECT_LABEL, STORY_LABEL,
            Collections.singletonList(STORY_SESSION_NAME)));
    }

    private static Set<String> namesTheDefinitionCarries() {
        return new LinkedHashSet<>(Collections.singletonList(STORY_SESSION_NAME));
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
    public void everyDrawnSessionRowNamesEitherALiveSessionOrANameTheDefinitionCarries() {
        List<String> liveSessionNames = Arrays.asList(STORY_SESSION_NAME,
            "https://example.test/live-session-outside-the-definition");
        Set<String> definedNames = namesTheDefinitionCarries();

        List<SessionHierarchyRow> rows =
            builder.build(liveSessionNames, definitionWithOneStorySession(), NA);

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
        List<String> liveSessionNames = Arrays.asList(STORY_SESSION_NAME, liveSessionOutsideTheDefinition);

        List<SessionHierarchyRow> rows =
            builder.build(liveSessionNames, definitionWithOneStorySession(), NA);

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
